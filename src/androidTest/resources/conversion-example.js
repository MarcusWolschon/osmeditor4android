// convert a GeoJSON feature from the warnings format on qa.poole.ch/addresses/ch to a todo

// add the properties as text
var properties = feature.properties();
var content = new java.lang.StringBuilder();
if (properties != null) {
   for (var key in Iterator(feature.properties().keySet())) {   
       content.append(key + " " + feature.properties().get(key).getAsString() + "<BR>");
   } 
}
todo.setTitle(content.toString());
 
// add the id of the OSM element to the todo
var osmGeom = feature.properties().get("OSM geometry").getAsString();
var osmId =  feature.properties().get("OSM id").getAsLong();

var elementList = new LongPrimitiveList();
if (osmGeom == "point") {
   elementList.add(osmId);
   todo.setNodes(elementList); 
} else if (osmGeom == "polygon") {
   if (osmId < 1) {
       elementList.add(-osmId);
       todo.setRelations(elementList);
   } else {
       elementList.add(osmId);
       todo.setWays(elementList);
   }
}

// set the location of the todo 
var geometry = feature.geometry();
todo.setLat(Math.round(geometry.latitude() * 1E7));
todo.setLon(Math.round(geometry.longitude() * 1E7));