# Align background

Many different background sources are available for OpenStreetMap editors, particularly satellite and aerial imagery however often suffers from small to large offsets from reality. It is important to check and potentially adjust for this **before** tracing anything for inclusion in to OpenStreetMap in every editor. Vespucci provides functions do this both from stored offsets and manually.

All operations affect only the current background layer.

### Manually adjusting the offset

To adjust the offset manually you need to align it to a reference, this could be a known numeric coordinate offset, GPS traces (from the corresponding overlay) or simply the existing OpenStreetMap data. In the "Align background" screen you can simply drag the background to the desired position, overlays and the OpenStreetMap data will remain stationary. In the upper left hand corner you will see a display indicating current zoom level and the current offset.

Existing the screen with "Done" will semi-permanently store the offset for this zoom level, to apply it to all zoom levels select "Apply (all Zooms)" from the menu.

### Retrieving an offset from the imagery offset DB

*(requires network connectivity)*

Select "FROM DB" to retrieve an offset from the imagery offset DB, this searches in a radius of 10km for offsets that are applicable to the current background layer. If offsets are found they will be presented in a nearest first order, with the options to either skip to the next one, apply the offset to all zooms, or just as stored in the offset DB.

### Saving an offset to the imagery offset DB

*(requires network connectivity)*

A manually set offset can be saved to the DB. Vespucci will create separate entries for each distinct offset and set of zooms, you can then individually decide to save or not. Please state in your description clearly what wou used as a reference for your offset.

### All functions

 * **FROM DB** - retrieve offset from imagery offset DB
 * **RESET** - reset the offset to what is was before the "Align background" mode was started
 * **ZERO** - set the offset to zero
 * **Apply (all Zooms)** - apply the current offset to all zoom levels
 * **Save to DB** - save to imagery offset DB
 * **Help** - start the Vespucci help browser
  