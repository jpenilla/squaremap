var world = "world";
var curZoom = 0;
var maxZoom = 0;

var settings = {
    defZoom: 3,
    minZoom: 0,
    maxZoom: 3,
    extraZoomIn: 2
};

var map = L.map('map', {
    crs: L.CRS.Simple,
    center: [0, 0],
    attributionControl: false,
    noWrap: true
})
.addEventListener('zoomEnd', (event) => {
    //
});

var layerControls = {};


// init
// TODO start with getJson of global settings file
init();
centerFromURL();


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

// setup the map
function init() {
    map
        .setView([0, 0], settings.defZoom)
        .setMinZoom(settings.minZoom) // extra zoom out doesn't work :(
        .setMaxZoom(settings.maxZoom + settings.extraZoomIn)
    ;

    // setup the map tiles layer
    L.tileLayer("tiles/" + world + "/{z}/{x}_{y}.png", {
        tileSize: 512,
        minNativeZoom: settings.minZoom,
        maxNativeZoom: settings.maxZoom
    }).addTo(map);

    tick(0);
}


function unproject(x, z) {
    return map.unproject([x, z], settings.maxZoom);
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
        if (settings.world.player_tracker.nameplates.enabled) {
            var tooltip = L.tooltip({
                permanent: true,
                direction: "right",
                offset: [10,0],
                pane: "nameplate"
            });
            if (settings.world.player_tracker.nameplates.show_heads) {
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
        coords.updateHTML(map.project(event.latlng, settings.maxZoom));
    });
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
    if (y == null) y = settings.defZoom;
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
            settings.world = json.settings;
            if (count == 0) {
                // TODO move all this to global json init
                document.title = settings.world.ui.title;
                spawn.init();
                players.init();
                L.control.layers({}, layerControls, {position: 'topleft'}).addTo(map);
                if (settings.world.ui.coordinates) {
                    addUICoordinates();
                }
                addPlayerList();
            }
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
