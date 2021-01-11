var world = getUrlParam("world", "world");

var worlds = {};
var settings = {};
var layerControls = {};
var tileLayer;


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


// get worlds.json and setup worlds list
function init() {
    getJSON("worlds.json", function(json) {
        worlds = json.worlds;
        loadWorld(true);
    });
}


// get world settings.json and init the map
var firstLoad = true;
function loadWorld(centerOnSpawn) {
    getJSON("tiles/" + world + "/settings.json", function(json) {
        settings = json.settings;

        // setup page title
        document.title = settings.ui.title;

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
        tiles = L.tileLayer("tiles/" + world + "/{z}/{x}_{y}.png", {
            tileSize: 512,
            minNativeZoom: 0,
            maxNativeZoom: settings.zoom.max
        }).addTo(map);
        tileLayer = tiles;

        if (centerOnSpawn) {
            centerOn(settings.spawn.x, settings.spawn.z, settings.zoom.def);
        }

        if (firstLoad) {
            // setup view
            var zoom = getUrlParam("zoom", settings.zoom.def);
            var x = getUrlParam("x", settings.spawn.x);
            var z = getUrlParam("z", settings.spawn.z);
            centerOn(x, z, zoom)
                .setMinZoom(0) // extra zoom out doesn't work :(
                .setMaxZoom(settings.zoom.max + settings.zoom.extra);

            firstLoad = false;

            // init spawn marker
            spawn.init();

            // init player markers
            playerList.init();

            // add layer controls
            L.control.layers({}, layerControls, {position: 'topleft'}).addTo(map);
            if (settings.ui.coordinates) {
                addUICoordinates();
                addUILink();
            }

            // add sidebar
            addSidebar();

            // start the tick loop
            tick(0);
        } else {
            // update spawn marker
            spawn.update();
        }
    });
}


// spawn point
var spawn = {
    layer: new L.LayerGroup(),
    spawn: L.marker([0, 0], {icon: Icons.spawn}),
    init: function() {
        this.spawn
            .bindPopup("Spawn")
            .addTo(this.layer);
        layerControls.Spawn = this.layer.addTo(map);
        this.update();
    },
    update: function() {
        this.spawn.setLatLng(unproject(settings.spawn.x, settings.spawn.z));
    }
}


// player tracker
var playerList = {
    layer: new L.LayerGroup(),
    players: {},
    markers: new Map(),
    entries: new Map(),
    init: function() {
        layerControls.Player = this.layer.addTo(map);
        map.createPane("nameplate").style.zIndex = 1000;
    },
    addPlayerMarker: function(player) {
        var marker = L.marker(unproject(player.x, player.z), {
            icon: Icons.player,
            rotationAngle: (180 + player.yaw)
        }).addTo(this.layer);
        if (settings.player_tracker.nameplates.enabled) {
            var tooltip = L.tooltip({
                permanent: true,
                direction: "right",
                offset: [10,0],
                pane: "nameplate"
            });
            if (settings.player_tracker.nameplates.show_heads) {
                tooltip.setContent("<img src='" + getHeadUrl(player) + "' /><span>" + player.name + "</span>");
            } else {
                tooltip.setContent("<span>" + player.name + "</span>");
            }
            marker.bindTooltip(tooltip);
        }
        this.markers.set(player.uuid, marker);
    },
    addPlayerEntry: function(player) {
        var head = document.createElement("img");
        head.src = getHeadUrl(player);
        var span = document.createElement("span");
        span.appendChild(document.createTextNode(player.name));
        var link = document.createElement("a");
        link.id = player.uuid;
        link.onclick = function() { showPlayer(this); };
        link.appendChild(head);
        link.appendChild(span);
        var fieldset = document.getElementById("players");
        fieldset.appendChild(link);
        this.entries.set(player.uuid, link);
    },
    removeMarker: function(uuid) {
        var marker = this.markers.get(uuid);
        if (marker != null) {
            map.removeLayer(marker);
            this.markers.delete(uuid);
        }
    },
    removePlayer: function(uuid) {
        playerEntry = document.getElementById(uuid);
        if (playerEntry != null) {
            playerEntry.remove();
        }
    },
    updateAll: function() {
        var markersToRemove = Array.from(this.markers.keys());
        var entriesToRemove = Array.from(this.players);
        for (var i = 0; i < this.players.length; i++) {
            var player = this.players[i];

            var marker = this.markers.get(player.uuid);
            if (player.world == world) {
                if (marker == null) {
                    markersToRemove.remove(player.uuid);
                    this.addPlayerMarker(player);
                } else {
                    markersToRemove.remove(player.uuid);
                    marker.setLatLng(unproject(player.x, player.z));
                    marker.setRotationAngle(180 + player.yaw);
                }
            }

            var entry = this.entries.get(player.uuid);
            if (entry == null) {
                entriesToRemove.remove(player.uuid);
                this.addPlayerEntry(player);
            } else {
                entriesToRemove.remove(player.uuid);
            }
        }
        for (var i = 0; i < markersToRemove.length; i++) {
            this.removeMarker(markersToRemove[i]);
        }
        for (var i = 0; i < entriesToRemove.length; i++) {
            this.removePlayer(entriesToRemove[i]);
        }
    }
}


// coordinates box
function addUICoordinates() {
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
    map.addControl(coords);
    map.addEventListener('mousemove', (event) => {
        coords.updateHTML(project(event.latlng));
    });
}


// share link box
function addUILink() {
    let Link = L.Control.extend({
        _container: null,
        options: {
            position: 'bottomleft'
        },
        onAdd: function (map) {
            var link = L.DomUtil.create('div', 'leaflet-control-layers link');
            this._link = link;
            this.updateHTML(null);
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


// sidebar
function addSidebar() {
    var sidebar = document.createElement("div");
    sidebar.id = "sidebar";

    var top = document.createElement("fieldset");
    top.id = "worlds";
    var topLegend = document.createElement("legend");
    topLegend.appendChild(document.createTextNode("Worlds"));
    top.appendChild(topLegend);

    var bottom = document.createElement("fieldset");
    bottom.id = "players";
    var bottomLegend = document.createElement("legend");
    bottomLegend.appendChild(document.createTextNode("Players"));
    bottom.appendChild(bottomLegend);

    sidebar.appendChild(top);
    sidebar.appendChild(bottom);

    document.getElementById("map").appendChild(sidebar);

    for (var i = 0; i < worlds.length; i++) {
        var link = document.createElement("a");
        link.id = worlds[i].name;
        var img = document.createElement("img");
        var span = document.createElement("span");

        link.onclick = function() { showWorld(this.id, true); };
        switch(worlds[i].type) {
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
        span.appendChild(document.createTextNode(worlds[i].name))

        link.appendChild(img);
        link.appendChild(span);
        top.appendChild(link);
    }
}


// switch map to a world without reloading page
function showWorld(world, centerOnSpawn) {
    if (this.world == world) {
        centerOn(settings.spawn.x, settings.spawn.z, settings.zoom.def);
        return;
    }
    this.world = world;
    loadWorld(centerOnSpawn);
    updateBrowserUrl(getUrlFromView());
}


// center view on specific player
function showPlayer(link) {
    var uuid = link.id;
    for (var i = 0; i < playerList.players.length; i++) {
        var player = playerList.players[i];
        if (uuid == player.uuid) {
            showWorld(player.world, false);
            map.panTo(unproject(player.x, player.z));
        }
    }
}


// center on new point
function centerOn(x, z, zoom) {
    return map.setView(unproject(x, z), zoom);
}


// tick the map
function tick(count) {
    getJSON("players.json", function(json) {
        playerList.players = json.players;
        playerList.updateAll();
    });
    setTimeout(function() {
        tick(++count);
    }, 1000);
}


// convert coords to latlng
function unproject(x, z) {
    return map.unproject([x, z], settings.zoom.max);
}


// convert latlng to point
function project(latlng) {
    return map.project(latlng, settings.zoom.max);
}


// get player's head url
function getHeadUrl(player) {
    return settings.player_tracker.nameplates.heads_url
            .replaceAll("{uuid}", player.uuid)
            .replaceAll("{name}", player.name);
}


// get a json object from url
function getJSON(url, fn) {
    fetch(url)
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
    return "?world=" + world + "&zoom=" + zoom + "&x=" + x + "&z=" + z;
}


// update the url in browser address bar without reloading page
function updateBrowserUrl(url) {
    window.history.pushState(null, "", url);
}


// Array Remove - By John Resig (MIT Licensed)
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
