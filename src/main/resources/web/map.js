var world = "world";
var curZoom = 0;
var maxZoom = 0;

var settings = {};
var layerControls = {};

var map = L.map('map', {
    crs: L.CRS.Simple,
    center: [0, 0],
    attributionControl: false,
    noWrap: true
});


// init
// TODO start with getJson of global settings file
init();


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

// init the json
function init() {
    getJSON("tiles/" + world + "/settings.json", function(json) {
        settings = json.settings;

        document.title = json.settings.ui.title;

        initMap();
        centerFromURL();

        spawn.init();
        players.init();

        L.control.layers({}, layerControls, {position: 'topleft'}).addTo(map);
        if (settings.ui.coordinates) {
            addUILink();
            addUICoordinates();
        }
        addPlayerList();

        spawn.update(json.spawn);
    });
}

// setup the map
function initMap() {
    map
        .setView([0, 0], settings.zoom.def)
        .setMinZoom(0) // extra zoom out doesn't work :(
        .setMaxZoom(settings.zoom.max + settings.zoom.extra)
    ;

    // setup the map tiles layer
    L.tileLayer("tiles/" + world + "/{z}/{x}_{y}.png", {
        tileSize: 512,
        minNativeZoom: 0,
        maxNativeZoom: settings.zoom.max
    }).addTo(map);

    tick(0);
}


function unproject(x, z) {
    return map.unproject([x, z], settings.zoom.max);
}

function project(latlng) {
    return map.project(latlng, settings.zoom.max);
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
    },
    update: function(point) {
        this.spawn.setLatLng(unproject(point.x, point.z));
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
                tooltip.setContent("<img src='https://crafatar.com/avatars/" + player.uuid + "?size=16&default=MHF_Steve&overlay' /><span>" + player.name + "</span>");
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


// ui stuff
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

function addPlayerList() {
    var sidebar = document.createElement("div");
    sidebar.id = "sidebar";

    var newContent = document.createTextNode("Hi there and greetings!");

    sidebar.appendChild(newContent);

    document.getElementById("map").appendChild(sidebar);

}


function centerFromURL() {
    var w = getUrlParam("world");
    if (w == null) w = world;
    var y = getUrlParam("zoom");
    if (y == null) y = settings.zoom.def;
    var x = getUrlParam("x");
    if (x == null) x = 0;
    var z = getUrlParam("z");
    if (z == null) z = 0;
    map.setView(unproject(x, z), y);
}

function getUrlParam(query) {
    var url = window.location.search.substring(1);
    var vars = url.split('&');
    for (var i = 0; i < vars.length; i++) {
        var param = vars[i].split('=');
        if (param[0] === query) {
            var value = param[1] === undefined ? '' : decodeURIComponent(param[1]);
            return value === '' ? null : value;
        }
    }
}

function getJSON(url, fn) {
    fetch(url)
        .then(async res => {
            if (res.ok) {
                fn(await res.json());
            }
        });
}

function tick(count) {
    if (count % 5 == 0) {
        getJSON("tiles/" + world + "/settings.json", function(json) {
            spawn.update(json.spawn);
        });
    }
    getJSON("tiles/" + world + "/players.json", function(json) {
        players.updateAll(json.players);
    });
    setTimeout(function() {
        tick(++count);
    }, 1000);
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
