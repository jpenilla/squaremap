var world = "world";
var settings = null;
var layerControls = {};

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

// data - TODO phase this out, replace with json settings
var data = {
    title: "Pl3xMap",
    tileSize: 512,
    centerX: 0,
    centerZ: 0,
    defZoom: 3,
    minZoom: 0,
    maxZoom: 3,
    extraZoomIn: 2,
    extraZoomOut: 0, /* does not work! */
    spawnX: 0,
    spawnZ: 0,
}


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
var map = L.map('pl3xmap', {
  attributionControl: false,
  crs: L.CRS.Simple,
  center: [data.centerX, data.centerZ],
  zoom: data.defZoom,
  minZoom: (data.minZoom - data.extraZoomOut),
  maxZoom: (data.maxZoom + data.extraZoomIn),
  noWrap: true
});

// setup the map tiles layer
L.tileLayer('tiles/world/{z}/{x}_{y}.png', {
  tileSize: data.tileSize,
  minNativeZoom: data.minZoom,
  maxNativeZoom: data.maxZoom
}).addTo(map);



// spawn point
var spawnLayer = new L.LayerGroup();

var spawn = L.marker(map.unproject([data.spawnX, data.spawnZ], data.maxZoom), {
    icon: Icons.spawn
})
.bindPopup("Spawn")
.addTo(spawnLayer);

spawnLayer.addTo(map);
layerControls.Spawn = spawn;

function updateSpawn(point) {
    spawn.setLatLng(map.unproject([point.x, point.z], data.maxZoom));
}

// player tracker
var playersLayer = new L.LayerGroup();
playersLayer.addTo(map);
layerControls.Players = playersLayer;

var playerMarkers = new Map();

map.createPane("nameplate").style.zIndex = 1000;

function addPlayer(player) {
    var marker = L.marker(map.unproject([player.x, player.z], data.maxZoom), {
        icon: Icons.player,
        rotationAngle: (180 + player.yaw)
    }).addTo(playersLayer);
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
    playerMarkers.set(player.uuid, marker);
}

function removePlayer(uuid) {
    var marker = playerMarkers.get(uuid);
    if (marker != null) {
        map.removeLayer(marker);
        playerMarkers.delete(uuid);
    }
}

function updatePlayer(marker, player) {
    marker.setLatLng(map.unproject([player.x, player.z], data.maxZoom));
    marker.setRotationAngle(180 + player.yaw);
}

function updatePlayers(players) {
    var toRemove = Array.from(playerMarkers.keys());
    for (var i = 0; i < players.length; i++) {
        var player = players[i];
        var marker = playerMarkers.get(player.uuid);
        if (marker != null) {
            toRemove.remove(player.uuid);
            updatePlayer(marker, player);
        } else {
            toRemove.remove(player.uuid);
            addPlayer(player);
        }
    }
    for (var i = 0; i < toRemove.length; i++) {
        removePlayer(toRemove[i]);
    }
}


// ui stuff
L.control.layers({}, layerControls, {
    position: 'topleft'
}).addTo(map);

function addUICoordinates() {
    let Coords = L.Control.extend({
        _container: null,
        options: {
            position: 'topleft'
        },

        onAdd: function (map) {
            var coords = L.DomUtil.create('div', 'leaflet-control-layers coordinates');
            this._coords = coords;
            this.updateHTML("---", "---");
            return coords;
        },

        updateHTML: function(lat, lng) {
            var coords = lat + ", " + lng;
            this._coords.innerHTML = "Coordinates<br />" + coords;
        }
    });
    var coords = new Coords();
    map.addControl(coords);

    map.addEventListener('mousemove', (event) => {
        coord = map.project(event.latlng, data.maxZoom);
        coords.updateHTML(coord.x, coord.y);
    });
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
            settings = json.settings;
            if (count == 0) {
                document.title = settings.ui.title;
                if (settings.ui.coordinates) {
                    addUICoordinates();
                }
            }
            updateSpawn(json.spawn);
        });
    }
    getJSON("tiles/" + world + "/players.json", function(json) {
        updatePlayers(json.players);
    });
    setTimeout(function() {
        tick(++count);
    }, 1000);
}
tick(0);