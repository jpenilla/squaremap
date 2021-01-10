var world = getUrlParam("world", "world");

var settings = {};
var layerControls = {};


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


// get settings.json and init the map
function init() {
    getJSON("tiles/" + world + "/settings.json", function(json) {
        settings = json.settings;

        document.title = json.settings.ui.title;

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
        L.tileLayer("tiles/" + world + "/{z}/{x}_{y}.png", {
            tileSize: 512,
            minNativeZoom: 0,
            maxNativeZoom: settings.zoom.max
        }).addTo(map);

        var zoom = getUrlParam("zoom", settings.zoom.def);
        var x = getUrlParam("x", 0);
        var z = getUrlParam("z", 0);

        map.setView(unproject(x, z), zoom)
            .setMinZoom(0) // extra zoom out doesn't work :(
            .setMaxZoom(settings.zoom.max + settings.zoom.extra);

        spawn.init();
        players.init();

        L.control.layers({}, layerControls, {position: 'topleft'}).addTo(map);
        if (settings.ui.coordinates) {
            addUICoordinates();
            addUILink();
        }

        addSidebar(settings.worlds);

        tick(0);
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
        this.spawn.setLatLng(unproject(settings.spawn.x, settings.spawn.z));
    }
}


// player tracker
var players = {
    layer: new L.LayerGroup(),
    markers: new Map(),
    init: function() {
        layerControls.Player = this.layer.addTo(map);
        map.createPane("nameplate").style.zIndex = 1000;
    },
    addPlayer: function(player) {
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
                var url = settings.player_tracker.nameplates.heads_url
                    .replaceAll("{uuid}", player.uuid)
                    .replaceAll("{name}", player.name);
                tooltip.setContent("<img src='" + url + "' /><span>" + player.name + "</span>");
            } else {
                tooltip.setContent("<span>" + player.name + "</span>");
            }
            marker.bindTooltip(tooltip);
        }
        this.markers.set(player.uuid, marker);
    },
    removePlayer: function(uuid) {
        var marker = this.markers.get(uuid);
        if (marker != null) {
            map.removeLayer(marker);
            this.markers.delete(uuid);
        }
    },
    updatePlayer: function(marker, player) {
        marker.setLatLng(unproject(player.x, player.z));
        marker.setRotationAngle(180 + player.yaw);
    },
    updateAll: function(players) {
        var toRemove = Array.from(this.markers.keys());
        for (var i = 0; i < players.length; i++) {
            var player = players[i];
            var marker = this.markers.get(player.uuid);
            if (marker != null) {
                toRemove.remove(player.uuid);
                this.updatePlayer(marker, player);
            } else {
                toRemove.remove(player.uuid);
                this.addPlayer(player);
            }
        }
        for (var i = 0; i < toRemove.length; i++) {
            this.removePlayer(toRemove[i]);
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
            var center = project(map.getCenter());
            var zoom = map.getZoom();
            var x = Math.floor(center.x);
            var z = Math.floor(center.y);
            var url = "?world=" + world + "&zoom=" + zoom + "&x=" + x + "&z=" + z
            this._link.innerHTML = "<a href='" + url + "'><img src='images/clear.png'/></a>";
        }
    });
    var link = new Link();
    map.addControl(link);
    map.addEventListener('move', (event) => link.updateHTML());
    map.addEventListener('zoom', (event) => link.updateHTML());
}

// sidebar
function addSidebar(worlds) {
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
        var img = document.createElement("img");
        var span = document.createElement("span");

        link.href = "?world=" + worlds[i].name;
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


// tick the map
function tick(count) {
    getJSON("tiles/" + world + "/players.json", function(json) {
        players.updateAll(json.players);
    });
    setTimeout(function() {
        tick(++count);
    }, 1000);
}

// start it up
init();


// helper functions
function unproject(x, z) {
    return map.unproject([x, z], settings.zoom.max);
}

function project(latlng) {
    return map.project(latlng, settings.zoom.max);
}

function getJSON(url, fn) {
    fetch(url)
        .then(async res => {
            if (res.ok) {
                fn(await res.json());
            }
        });
}

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
