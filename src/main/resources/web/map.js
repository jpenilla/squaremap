document.title = data.title;
var map = L.map('mapid',{
  attributionControl: false,
  crs: L.CRS.Simple,
  center: [data.centerX, data.centerZ],
  zoom: data.defZoom,
  minZoom: data.minZoom,
  maxZoom: data.maxZoom,
  noWrap: true
});
L.tileLayer('tiles/world/{z}/{x}_{y}.png', {
  tileSize: 512
}).addTo(map);