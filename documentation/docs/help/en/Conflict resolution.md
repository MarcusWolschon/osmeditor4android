# Conflict Resolution

Vespucci provides a basic facility to resolve version conflicts on upload. In general is it best to avoid them happening, particularly in areas with lots of editing activity download only small areas, refresh them often and save early.

Vespucci uploads modified and deleted elements one by one. Any conflicts detected will be related to the current element and will need to be resolved before the upload can be retried. As a consequence, if you suspect that your edits have caused more than a handful of conflicts you should save the edits either by saving to a JOSM compatible OSM file or by exporting the changes to a OSC file and then try to resolve the conflicts in JOSM.

### Version conflicts

The most common conflict is when an object has been modified or deleted while you were editing and the version number of the object that you modified is lower than the current one on the OpenStreetMap servers. In such a case you will be presented with the options to cancel, use your local version or to overwrite your changes with whatever is on the server.

It is likely better, except if you have really important changes, to use the remote version. If you use the local version you will overwrite whatever changes have been made by other mappers. Note that if the object has been deleted on the server and you choose to use the server version, with other words, delete the object locally, this may cause further conflicts if the object in question was a member of a way or relation.

### Referential conflicts

While Vespucci in general doesn't allow this to happen, a referential conflict happens when you delete an object locally, the server however still has objects that refer to the deleted one, which haven't been modified accordingly. Deletes happen at the end of the upload process for this reason to allow the server to check. In such a conflict situation you only have the choice to use the server version or cancel.  