# Image and photo viewer

The image viewer lets you browse images stored on your device and images loaded from supported online sources such as Mapillary and Panoramax. It can be opened from the photo layer, from a selected image, or after importing a photo into Vespucci.

If supported on the device the view will be opened in a separate window (on earlier Android versions this may require manually creating a dual window screen before using the viewer). You can disable using the internal viewer completely in the [Advanced preferences](Advanced%20preferences.md#use-builtin-photo-viewer).   

## Actions

Some actions are only visible if they can actually be executed for the current image and context.

### Back

Move to the previous image in the current sequence.

### Forward

Move to the next image in the current sequence.

### Share

Share the current image with another app on the device.

### Go to photo

Center the map on the location of the current image and, if available, select it in the photo layer.

### Information

Display detailed metadata for the current image, including location and technical information when available. For Panoramax and Mapillary images a clickable and sharable link to the the original image will be displayed.  

### Upload

Upload the current image to a configured image storage service. Currently supported targets include Panoramax and Wikimedia Commons. You need to configure and authorize access in the [Advanced preferences](Advanced%20preferences.md#image-storage). *(requires network connectivity)*

### Add link to element

Add a link from the current image to the selected OSM element. This is supported for Mapillary and Panoramax images and adds the appropriate OSM tag to the selected element.

This action is only available when exactly one OSM element is selected. Vespucci will add the relevant tag for the image source so the feature can link back to the photo.

Use this to attach an image reference to a feature while reviewing photo sources in the viewer. *(requires a selected OSM element and network access for the source service)*

### Delete

Remove the current image from the device or from the photo layer index, depending on the source. *(requires storage permissions for the underlying file)*

## Uploading from the image viewer

When uploading an image from the image viewer, Vespucci can add the corresponding OSM link to the selected element as part of the upload flow. If no element is selected, the upload can still be completed normally, and the link can be added when associating the image with a selected feature.

This is useful when you want to attach a photo directly to a feature while reviewing images in the viewer, without first returning to the map editor.
