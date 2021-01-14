var settings = {};

var layerControls = {};
var tileLayer;

var sidebar;
var worldList;
var playerList;


// the map
var mapId = "map";
var map = L.map(mapId, {
    crs: L.CRS.Simple,
    center: [0, 0],
    attributionControl: false,
    noWrap: true
});


// icons
var Icons = {
    spawn: L.icon({
        iconUrl: 'images/spawn.png',
        iconSize: [16, 16],
        iconAnchor: [8, 8],
        popupAnchor: [0, -10]
    }),
    player: L.icon({
        iconUrl: 'images/player.png',
        iconSize: [17, 16],
        iconAnchor: [8, 9],
        tooltipAnchor: [0, 0]
    })
};


// start it up
init();


// get settings.json and setup worlds list
function init() {
    getJSON("tiles/settings.json", function(json) {
        settings.ui = json.ui;

        sidebar = new Sidebar();

        playerList = new PlayerList();

        worldList = new WorldList(json.worlds);

        worldList.loadWorld(getUrlParam("world", "world"), function(world) {
            // setup view
            centerOn(getUrlParam("x", world.spawn.x),
                    getUrlParam("z", world.spawn.z),
                    getUrlParam("zoom", world.zoom.def))
                .setMinZoom(0) // extra zoom out doesn't work :(
                .setMaxZoom(world.zoom.max + world.zoom.extra);

            // add layer controls
            L.control.layers({}, layerControls, {position: 'topleft'}).addTo(map);
            if (settings.ui.coordinates) {
                new UICoordinates();
            }
            if (settings.ui.link) {
                new UILink();
            }

            // start the tick loop
            tick(0);
        });
    });
}






// #######################################################################################################






class WorldList {
    constructor(json) {
        this.worlds = new Map();

        for (var i = 0; i < json.length; i++) {
            var world = new World(json[i]);
            this.worlds.set(world.name, world);

            var link = createElement("a", world.name, this);
            link.onclick = function() {
                var curWorld = this.parent.curWorld;
                var name = this.id;
                if (curWorld.name == name) {
                    centerOn(curWorld.spawn.x, curWorld.spawn.z, curWorld.zoom.def);
                    return;
                }
                playerList.clearMarkers();
                this.parent.loadWorld(name);
            };

            var img = document.createElement("img");
            switch(world.type) {
                case "nether":
                    img.src = "images/red-cube-smol.png";
                    break;
                case "the_end":
                    img.src = "images/purple-cube-smol.png";
                    break;
                case "normal":
                default:
                    img.src = "images/green-cube-smol.png";
                    break;
            }

            link.appendChild(img);
            link.appendChild(createTextElement("span", world.display_name));

            sidebar.worldList.element.appendChild(link);
        }
    }
    loadWorld(name, callback) {
        // unload current world
        if (this.curWorld != null) {
            this.curWorld.unload();
        }

        // load new world
        var world = this.worlds.get(name);
        this.curWorld = world;
        world.load(callback);
    }
    showWorld(world, callback) {
        if (this.curWorld.name == world) {
            centerOn(this.curWorld.spawn.x, this.curWorld.spawn.z, this.curWorld.zoom.def);
            if (callback != null) {
                callback();
            }
            return;
        }
        playerList.clearMarkers();
        this.loadWorld(world, callback);
        updateBrowserUrl(getUrlFromView());
    }
}


class World {
    constructor(world) {
        this.name = world.name;
        this.type = world.type;
        this.display_name = world.display_name;
        this.spawn = new Spawn(this);
    }
    load(callback) {
        getJSON("tiles/" + this.name + "/settings.json", function(json) {
            worldList.curWorld.__load(json, callback);
        });
    }
    unload() {
        this.spawn.hide();
    }
    // "internal" function so we get a proper scope for "this"
    __load(json, callback) {
            this.player_tracker = json.settings.player_tracker;
            this.zoom = json.settings.zoom;

            // setup page title
            document.title = settings.ui.title
                .replaceAll("{world}", json.display_name);

            // setup background
            var mapDom = document.getElementById(mapId);
            switch(json.type) {
                case "nether":
                    mapDom.style.background = "url('images/nether_sky.png')";
                    break;
                case "the_end":
                    mapDom.style.background = "url('images/end_sky.png')";
                    break;
                case "normal":
                default:
                    mapDom.style.background = "url('images/overworld_sky.png')";
                    break;
            }

            // setup the map tiles layer
            if (tileLayer != null) {
                map.removeLayer(tileLayer);
            }
            var tiles = L.tileLayer("tiles/" + this.name + "/{z}/{x}_{y}.png", {
                tileSize: 512,
                minNativeZoom: 0,
                maxNativeZoom: this.zoom.max
            }).addTo(map);
            tileLayer = tiles;

            // update and show spawn point
            this.spawn.x = json.settings.spawn.x;
            this.spawn.z = json.settings.spawn.z;
            this.spawn.show();

            // center on spawn
            centerOn(this.spawn.x, this.spawn.z, this.zoom.def);

            // force self update with existing player list
            playerList.update(Array.from(playerList.players.values()));

            if (callback != null) {
                callback(this);
            }
    }
}


class Spawn {
    constructor(world) {
        this.layer = new L.LayerGroup();
        this.spawn = L.marker([0, 0], {icon: Icons.spawn})
            .bindPopup(world.display_name + " Spawn")
            .addTo(this.layer);
        this.x = 0;
        this.z = 0;
    }
    show() {
        layerControls.Spawn = this.layer.addTo(map);
        this.spawn.setLatLng(unproject(this.x, this.z));
    }
    hide() {
        map.removeLayer(this.layer);
    }
}


class Sidebar {
    constructor() {
        this.sidebar = createElement("div", "sidebar", this);
        document.getElementById("map").appendChild(this.sidebar);

        if (settings.ui.sidebar != "hide") {
            this.pin = new Pin(settings.ui.sidebar == "pinned");
            this.sidebar.appendChild(this.pin.element);
            this.show(this.pin.pinned);
        }

        this.worldList = new Fieldset("worlds", "Worlds");
        this.sidebar.appendChild(this.worldList.element);

        this.playerList = new Fieldset("players", "Players");
        this.sidebar.appendChild(this.playerList.element);

        this.sidebar.onmouseleave = function() {
            if (!this.parent.pin.pinned) {
                this.parent.show(false);
            }
        };
        this.sidebar.onmouseenter = function() {
            if (!this.parent.pin.pinned) {
                this.parent.show(true);
            }
        };
    }
    show(show) {
        this.sidebar.className = show ? "show" : "";
    }
    updateWorlds() {
        worldList.update();
    }
    updatePlayers() {
        //
        //
    }
}


class Fieldset {
    constructor(id, title) {
        this.element = createElement("fieldset", id);
        var legend = createTextElement("legend", title);
        this.element.appendChild(legend);
    }
}


class Pin {
    constructor(def) {
        this.pinned = def;

        this.element = createElement("img", "pin", this);

        this.element.onclick = function() {
            this.parent.toggle();
        }

        this.pin(this.pinned);
    }
    toggle() {
        this.pin(!this.pinned);
    }
    pin(pin) {
        this.pinned = pin;
        this.element.className = pin ? "pinned" : "unpinned";
        this.element.src = "images/" + this.element.className + ".png";
    }
}


class PlayerList {
    constructor() {
        this.layer = new L.LayerGroup();
        this.players = new Map();

        layerControls.Player = this.layer.addTo(map);
        map.createPane("nameplate").style.zIndex = 1000;
    }
    showPlayer(link) {
        var uuid = link.id;
        var keys = Array.from(playerList.players.keys());
        for (var i = 0; i < keys.length; i++) {
            var player = playerList.players.get(keys[i]);
            if (uuid == player.uuid) {
                if (player.world != world) {
                    worldList.showWorld(player.world, function () {
                        map.panTo(unproject(player.x, player.z));
                    });
                }
            }
        }
    }
    add(player) {
        var head = document.createElement("img");
        head.src = getHeadUrl(player);
        var span = createTextElement("span", player.name);
        var link = createElement("a", player.uuid, this);
        link.onclick = function() {
            this.parent.showPlayer(this);
        };
        link.appendChild(head);
        link.appendChild(span);
        var fieldset = sidebar.playerList.element;
        fieldset.appendChild(link);
    }
    remove(uuid) {
        var player = document.getElementById(uuid);
        if (player != null) {
            player.remove();
        }
    }
    update(players) {
        var playersToRemove = Array.from(this.players.keys());
        for (var i = 0; i < players.length; i++) {
            var player = this.players.get(players[i].uuid);
            if (player == null) {
                player = new Player(players[i]);
                this.players.set(player.uuid, player);
                this.add(player);
            }
            player.update(players[i]);
            playersToRemove.remove(players[i].uuid);
        }
        for (var i = 0; i < playersToRemove.length; i++) {
            var player = this.players.get(playersToRemove[i]);
            player.marker.remove();
            this.players.delete(player.uuid);
            this.remove(player.uuid);
        }
    }
    clearMarkers() {
        var keys = Array.from(this.players.keys());
        for (var i = 0; i < keys.length; i++) {
            var player = this.players.get(keys[i]);
            player.marker.remove();
        }
    }
}


class Player {
    constructor(player) {
        this.name = player.name;
        this.uuid = player.uuid;
        this.world = player.world;
        this.x = 0;
        this.z = 0;
        this.marker = L.marker(unproject(player.x, player.z), {
            icon: Icons.player,
            rotationAngle: (180 + player.yaw)
        });
        if (worldList.curWorld.player_tracker.nameplates.enabled) {
            var tooltip = L.tooltip({
                permanent: true,
                direction: "right",
                offset: [10,0],
                pane: "nameplate"
            });
            var headImg = "";
            if (worldList.curWorld.player_tracker.nameplates.show_heads) {
                headImg = "<img src='" + getHeadUrl(player) + "' />";
            }
            tooltip.setContent(headImg + "<span>" + player.name + "</span>");
            this.marker.bindTooltip(tooltip);
        }
    }
    update(player) {
        this.x = player.x;
        this.z = player.z;
        if (worldList.curWorld.name == player.world) {
            this.marker.addTo(playerList.layer);
            var latlng = unproject(player.x, player.z);
            if (!this.marker.getLatLng().equals(latlng)) {
                this.marker.setLatLng(latlng);
            }
            var angle = 180 + player.yaw;
            if (this.marker.options.rotationAngle != angle) {
                this.marker.setRotationAngle(angle);
            }
        } else {
            this.marker.remove();
        }
    }
}


class UICoordinates {
    constructor() {
        let Coords = L.Control.extend({
            _container: null,
            options: {
                position: 'bottomleft'
            },
            onAdd: function (map) {
                var coords = L.DomUtil.create('div', 'leaflet-control-layers coordinates');
                this._coords = coords;
                this.updateHTML();
                return coords;
            },
            updateHTML: function(point) {
                var x = point == null ? "---" : Math.round(point.x);
                var z = point == null ? "---" : Math.round(point.y);
                this._coords.innerHTML = "Coordinates<br />" + x + ", " + z;
            }
        });
        var coords = new Coords();
        map.addControl(coords);
        map.addEventListener('mousemove', (event) => {
            if (worldList.curWorld != null) {
                coords.updateHTML(project(event.latlng));
            }
        });
    }
}


class UILink {
    constructor() {
        let Link = L.Control.extend({
            _container: null,
            options: {
                position: 'bottomleft'
            },
            onAdd: function (map) {
                var link = L.DomUtil.create('div', 'leaflet-control-layers link');
                this._link = link;
                this.updateHTML();
                return link;
            },
            updateHTML: function() {
                var url = getUrlFromView();
                updateBrowserUrl(url);
                this._link.innerHTML = "<a href='" + url + "'><img src='images/clear.png'/></a>";
            }
        });
        var link = new Link();
        map.addControl(link);
        map.addEventListener('move', (event) => link.updateHTML());
        map.addEventListener('zoom', (event) => link.updateHTML());
    }
}





// #######################################################################################################






// tick the map
function tick(count) {
    getJSON("tiles/players.json", function(json) {
        playerList.update(json.players);
    });
    setTimeout(function() {
        tick(++count);
    }, 1000);
}

// center on new point
function centerOn(x, z, zoom) {
    return map.setView(unproject(x, z), zoom);
}


// convert coords to latlng
function unproject(x, z) {
    return map.unproject([x, z], worldList.curWorld.zoom.max);
}


// convert latlng to point
function project(latlng) {
    return map.project(latlng, worldList.curWorld.zoom.max);
}


// create new dom element with id and parent
function createElement(tag, id, parent) {
    var element = document.createElement(tag);
    element.id = id;
    element.parent = parent;
    return element;
}

// create simple element with text inside
function createTextElement(tag, text) {
    var element = document.createElement(tag);
    element.appendChild(document.createTextNode(text));
    return element;
}


// get player's head url
function getHeadUrl(player) {
    return worldList.curWorld.player_tracker.nameplates.heads_url
            .replaceAll("{uuid}", player.uuid)
            .replaceAll("{name}", player.name);
}


// get a json object from url
function getJSON(url, fn) {
    fetch(url, {cache: "no-store"})
        .then(async res => {
            if (res.ok) {
                fn(await res.json());
            }
        });
}


// get a param from browser's url
function getUrlParam(query, def) {
    var url = window.location.search.substring(1);
    var vars = url.split('&');
    for (var i = 0; i < vars.length; i++) {
        var param = vars[i].split('=');
        if (param[0] === query) {
            var value = param[1] === undefined ? '' : decodeURIComponent(param[1]);
            return value === '' ? def : value;
        }
    }
    return def;
}


// contrust a url from the map's current view
function getUrlFromView() {
    var center = project(map.getCenter());
    var zoom = map.getZoom();
    var x = Math.floor(center.x);
    var z = Math.floor(center.y);
    return "?world=" + worldList.curWorld.name + "&zoom=" + zoom + "&x=" + x + "&z=" + z;
}


// update the url in browser address bar without reloading page
function updateBrowserUrl(url) {
    window.history.pushState(null, "", url);
}


// https://stackoverflow.com/a/3955096
Array.prototype.remove = function() {
    var what, a = arguments, L = a.length, ax;
    while (L && this.length) {
        what = a[--L];
        while ((ax = this.indexOf(what)) !== -1) {
            this.splice(ax, 1);
        }
    }
    return this;
};
