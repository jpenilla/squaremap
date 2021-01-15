class Pl3xMap {
    constructor() {
        this.settings = {};

        this.map = L.map("map", {
            crs: L.CRS.Simple,
            center: [0, 0],
            attributionControl: false,
            noWrap: true
        })
        .addEventListener('move', (event) => {
            this.updateBrowserUrl(this.getUrlFromView());
        })
        .addEventListener('zoom', (event) => {
            this.updateBrowserUrl(this.getUrlFromView());
        });

        this.playersLayer = new L.LayerGroup()
            .addTo(this.map);
        this.spawnLayer = new L.LayerGroup()
            .addTo(this.map);

        this.tileLayer;

        this.sidebar;
        this.worldList;
        this.playerList;

        this.init();
    }
    tick() {
        this.getJSON("tiles/players.json", function(json) {
            P.playerList.update(json.players);
        });
        setTimeout(function() {
            P.tick();
        }, 1000);
    }
    init() {
        this.getJSON("tiles/settings.json", function(json) {
            P.__init(json);
        });
    }
    __init(json) {
        this.settings.ui = json.ui;
        this.sidebar = new Sidebar();
        this.playerList = new PlayerList();
        this.worldList = new WorldList(json.worlds);
        if (P.settings.ui.coordinates) {
            new UICoordinates();
        }
        this.worldList.loadWorld(this.getUrlParam("world", "world"), function(world) {
            // setup view
            P.centerOn(P.getUrlParam("x", world.spawn.x),
                    P.getUrlParam("z", world.spawn.z),
                    P.getUrlParam("zoom", world.zoom.def))
                .setMinZoom(0) // extra zoom out doesn't work :(
                .setMaxZoom(world.zoom.max + world.zoom.extra);

            // add layer controls
            L.control.layers({}, {
                Players: P.playersLayer,
                Spawn: P.spawnLayer
            }, {
                position: 'topleft'
            }).addTo(P.map);

            // start the tick loop
            P.tick();
        });
    }
    centerOn(x, z, zoom) {
        return this.map.setView(this.unproject(x, z), zoom);
    }
    unproject(x, z) {
        return this.map.unproject([x, z], this.worldList.curWorld.zoom.max);
    }
    project(latlng) {
        return this.map.project(latlng, this.worldList.curWorld.zoom.max);
    }
    createElement(tag, id, parent) {
        var element = document.createElement(tag);
        element.id = id;
        element.parent = parent;
        return element;
    }
    createTextElement(tag, text) {
        var element = document.createElement(tag);
        element.appendChild(document.createTextNode(text));
        return element;
    }
    getHeadUrl(player) {
        return this.worldList.curWorld.player_tracker.nameplates.heads_url
            .replaceAll("{uuid}", player.uuid)
            .replaceAll("{name}", player.name);
    }
    getJSON(url, fn) {
        fetch(url, {cache: "no-store"})
            .then(async res => {
                if (res.ok) {
                    fn(await res.json());
                }
            });
    }
    getUrlParam(query, def) {
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
    getUrlFromView() {
        var center = this.project(this.map.getCenter());
        var zoom = this.map.getZoom();
        var x = Math.floor(center.x);
        var z = Math.floor(center.y);
        return "?world=" + this.worldList.curWorld.name + "&zoom=" + zoom + "&x=" + x + "&z=" + z;
    }
    updateBrowserUrl(url) {
        window.history.pushState(null, "", url);
    }
}


class WorldList {
    constructor(json) {
        this.worlds = new Map();

        for (var i = 0; i < json.length; i++) {
            var world = new World(json[i]);
            this.worlds.set(world.name, world);

            var link = P.createElement("a", world.name, this);
            link.onclick = function() {
                var curWorld = this.parent.curWorld;
                var name = this.id;
                if (curWorld.name == name) {
                    P.centerOn(curWorld.spawn.x, curWorld.spawn.z, curWorld.zoom.def);
                    return;
                }
                P.playerList.clearMarkers();
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
            link.appendChild(P.createTextElement("span", world.display_name));

            P.sidebar.worldList.element.appendChild(link);
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
            P.centerOn(this.curWorld.spawn.x, this.curWorld.spawn.z, this.curWorld.zoom.def);
            if (callback != null) {
                callback();
            }
            return;
        }
        P.playerList.clearMarkers();
        this.loadWorld(world, callback);
        P.updateBrowserUrl(P.getUrlFromView());
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
        P.getJSON("tiles/" + this.name + "/settings.json", function(json) {
            P.worldList.curWorld.__load(json, callback);
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
        document.title = P.settings.ui.title
            .replaceAll("{world}", json.display_name);

        // setup background
        var mapDom = document.getElementById("map");
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
        if (P.tileLayer != null) {
            P.map.removeLayer(P.tileLayer);
        }
        P.tileLayer = L.tileLayer("tiles/" + this.name + "/{z}/{x}_{y}.png", {
            tileSize: 512,
            minNativeZoom: 0,
            maxNativeZoom: this.zoom.max
        }).addTo(P.map);

        // update and show spawn point
        this.spawn.x = json.settings.spawn.x;
        this.spawn.z = json.settings.spawn.z;
        this.spawn.show();

        // center on spawn
        P.centerOn(this.spawn.x, this.spawn.z, this.zoom.def);

        // force self update with existing player list
        P.playerList.update(Array.from(P.playerList.players.values()));

        if (callback != null) {
            callback(this);
        }
    }
}


class Spawn {
    constructor(world) {
        this.x = 0;
        this.z = 0;
        this.spawn = L.marker([this.x, this.z],{
            icon: L.icon({
                iconUrl: 'images/spawn.png',
                iconSize: [16, 16],
                iconAnchor: [8, 8],
                popupAnchor: [0, -10]
            })
        }).bindPopup(world.display_name + " Spawn");
    }
    show() {
        this.spawn.addTo(P.map);
        this.spawn.setLatLng(P.unproject(this.x, this.z));
    }
    hide() {
        P.map.removeLayer(this.spawn);
    }
}


class Sidebar {
    constructor() {
        this.sidebar = P.createElement("div", "sidebar", this);
        document.getElementById("map").appendChild(this.sidebar);

        if (P.settings.ui.sidebar != "hide") {
            this.pin = new Pin(P.settings.ui.sidebar == "pinned");
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
}


class Fieldset {
    constructor(id, title) {
        this.element = P.createElement("fieldset", id);
        var legend = P.createTextElement("legend", title);
        this.element.appendChild(legend);
    }
}


class Pin {
    constructor(def) {
        this.pinned = def;

        this.element = P.createElement("img", "pin", this);

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
        this.players = new Map();
        P.map.createPane("nameplate").style.zIndex = 1000;
    }
    showPlayer(link) {
        var uuid = link.id;
        var keys = Array.from(P.playerList.players.keys());
        for (var i = 0; i < keys.length; i++) {
            var player = P.playerList.players.get(keys[i]);
            if (uuid == player.uuid && player.world != world) {
                P.worldList.showWorld(player.world, function () {
                    P.map.panTo(P.unproject(player.x, player.z));
                });
            }
        }
    }
    add(player) {
        var head = document.createElement("img");
        head.src = P.getHeadUrl(player);
        var span = P.createTextElement("span", player.name);
        var link = P.createElement("a", player.uuid, this);
        link.onclick = function() {
            this.parent.showPlayer(this);
        };
        link.appendChild(head);
        link.appendChild(span);
        var fieldset = P.sidebar.playerList.element;
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
        this.marker = L.marker(P.unproject(player.x, player.z), {
            icon: L.icon({
                iconUrl: 'images/player.png',
                iconSize: [17, 16],
                iconAnchor: [8, 9],
                tooltipAnchor: [0, 0]
            }),
            rotationAngle: (180 + player.yaw)
        });
        if (P.worldList.curWorld.player_tracker.nameplates.enabled) {
            var tooltip = L.tooltip({
                permanent: true,
                direction: "right",
                offset: [10,0],
                pane: "nameplate"
            });
            var headImg = "";
            if (P.worldList.curWorld.player_tracker.nameplates.show_heads) {
                headImg = "<img src='" + P.getHeadUrl(player) + "' />";
            }
            tooltip.setContent(headImg + "<span>" + player.name + "</span>");
            this.marker.bindTooltip(tooltip);
        }
    }
    update(player) {
        this.x = player.x;
        this.z = player.z;
        if (P.worldList.curWorld.name == player.world) {
            this.marker.addTo(P.playersLayer);
            var latlng = P.unproject(player.x, player.z);
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
                this.updateHTML(null);
                return coords;
            },
            updateHTML: function(point) {
                var x = point == null ? "---" : Math.round(point.x);
                var z = point == null ? "---" : Math.round(point.y);
                this._coords.innerHTML = "Coordinates<br />" + x + ", " + z;
            }
        });
        var coords = new Coords();
        P.map.addControl(coords);
        P.map.addEventListener('mousemove', (event) => {
            if (P.worldList.curWorld != null) {
                coords.updateHTML(P.project(event.latlng));
            }
        });
    }
}


var P = new Pl3xMap();


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
