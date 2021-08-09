## Vespucci Mapbox-GL Style Support

Only one vector tile source is currently supported, all other source configuration is ignored.

Supported layers: _background_, _fill_, _fill-extrusion_, _line_, _symbol_, _circle_

_fill-extrusion_ layers are treated as flat fill layers

Unsupported layers: raster, heatmap, hillshade, sky

Unsupported attributes are ignored.

Font selection is not supported, we render with a standard Android system font.

None of the new "expression" functions are currently supported, however "old style" filter expressions are supported and interpolation linear, identity and exponential interpolation for numbers, colors and categories work for lots of the supported attributes.

#### Further caveats

While a number of styles and corresponding vector tile schemas have been tested and within the limits described on this page things work, following specific points should be taken in to account:

- the support was mainly intended for relatively light weight QA data overlays, as this is based on Android Canvas rendering and not a purpose specific OpenGL rendering implementation performance will in general be slow. This is further confounded by many sources providing tiles up to a maximum of zoom level 14, these then tend to be very large and often contain far more data than what would be needed for the current view.
- we've implemented some simple label and icon collision detection, however if a collision is detected, we simply doesn't render one of the colliding objects without attempting to relocate the offending symbol (collision avoidance). Further there is currently a hard wired limit of 200 objects that are handled by this, any further ones are not rendered (the choice of limit is not of any particular significance).
- there is currently no TileJSON support.

### Style attributes

|Key                        | Value     | Support   | Notes
|---------------------------|-----------|-----------|----------------------------------------------------------------
|__version__                |           | yes       | ignored
|__sprite__                 |           | yes       | 

### Layer attributes for supported layers

Value support:

- __l__ literal
- __z__ "legacy" zoom based interpolation function
- __m__ moustache replacement
- __f__ "legacy" filter expressions


|Key                        | Values    | Support   | Notes
|---------------------------|-----------|-----------|----------------------------------------------------------------
|_All layers_               |           |
|__minzoom__                | l         | yes
|__maxzoom__                | l         | yes
|__ref__                    | l         | yes       | missing from mapbox documentation
|__visibility__             | l         | yes
|__interactive__            | l         | yes       | missing from mapbox documentation          
|_background_               |           |            
|__background-color__       | l z       | yes
|__backgroud-opacity__      | l z       | yes
|__background-pattern__     | l z       | yes
|_Vector/geometry tile layers_ |           |
|__filter__                   | f         | yes
|__source-layer__             | l         | yes
|_fill_                     |           |
|__fill-antialias__         | l         | yes
|__fill-color__             | l z       | yes
|__fill_opacity__           | l z       | yes
|__fill-outline-color__     | l z       | yes
|__fill-pattern__           | l z       | yes
|__fill-sort-key__          |           | no
|__fill-translate__         | l z       | yes
|__fill-translate-anchor__  |           | no
|_fill-extrusion_                     |           |
|__fill-extrusion-base__              |           | no
|__fill-extrusion-color__             | l z       | yes
|__fill-extrusion-height__            |           | no
|__fill-extrusion-opacity__           | l z       | yes
|__fill-extrusion-pattern__           | l z       | yes
|__fill-extrusion-translate__         | l z       | yes
|__fill-extrusion-translate-anchor__  |           | no
|__fill-extrusion-vertical-gradient__ |           | no
|_line_                     |           |
|__line-blur__              |           | no        |
|__line-cap__               | l z       | yes
|__line_color__             | l z       | yes
|__line-dasharray__         | l         | yes
|__line-gap-width__         |           | no
|__line-gradient__          |           | no
|__line-join__              | l z       | yes
|__line-miter-limit__       |           | no
|__line-offset__            |           | no
|__line-opacity__           | l z       | yes
|__line-pattern__           |           | no
|__line-round-limit__       |           | no
|__line-sort-key__          |           | no
|__line-translate__         |           | no
|__line-translate-anchor__  |           | no
|__line-width__             | l z       | yes
|_symbol_                   |           |
|__icon-allow-overlap__     |           | no
|__icon-anchor__            | l z       | yes
|__icon-color__             |           | no
|__icon-halo-blur__         |           | no
|__icon-halo-color__        |           | no
|__icon-halo-width__        |           | no
|__icon-ignore-placement__  |           | no
|__icon-image__             | l m z     | yes 
|__icon-keep-upright__      |           | no
|__icon-offset__            | l z       | yes
|__icon-opacity__           |           | no
|__icon-optional__          |           | no
|__icon-padding__           |           | no
|__icon-pitch-alignment__   |           | no
|__icon-rotate__            | l z       | yes
|__icon-rotation-alignment__|           | no
|__icon-size__              | l z       | yes
|__icon-text-fit__          |           | no
|__icon-text-fit-padding__  |           | no
|__icon-translate__         |           | no
|__icon-translate-anchor    |           | no
|__symbol-avoid-edges__     |           | no
|__symbol-placement__       | l z       | yes
|__symbol-sort-key__        |           | no
|__symbol-spacing__         |           | no
|__symbol-z-order__         |           | no
|__text-allow-overlap__     |           | no
|__text-anchor__            | l z       | yes | just "top" and "bottom" supported
|__text-color__             | l z       | yes
|__text-field__             | l m       | yes
|__text-font__              |           | no
|__text-halo-blur__         |           | no
|__text-halo-color__        | l z       | yes
|__text-halo-width__        | l z       | yes
|__text-ignore-placement__  |           | no
|__text-justify__           | l z       | yes | "auto" not supported
|__text-keep-upright__      |           | no
|__text-letter-spacing__    | l z       | yes
|__text-line-height__       |           | no
|__text-max-angle__         |           | no
|__text-max-width__         | l z       | yes
|__text-offset__            | l z       | yes
|__text-opacity__           | l z       | yes
|__text-optional__          |           | no
|__text-padding__           |           | no
|__text-pitch-alignment__   |           | no
|__text-radial-offset__     |           | no
|__text-rotate__            |           | no
|__text-rotation-alignment__|           | no
|__text-size__              | l z       | yes
|__text-transform__         | l z       | yes
|__text-translate__         |           | no
|__text-translate-anchor__  |           | no
|__text-variable-anchor__   |           | no
|__test-writing-mode__      |           | no
|_circle_                   |           |
|_circle-blur_              |           | no
|_circle-color_             | l z       | yes
|_circle-opacity_           | l z       | yes
|_circle-pitch-alignment_   |           | no
|_circle-pitch-scale_       |           | no 
|_circle-radius_            | l z       | yes
|_circle-sort-key_          |           | no
|_circle-stroke-color_      | l z       | yes
|_circle-stroke-opacity_    | l z       | yes
|_circle-stroke-width_      | l z       | yes
|_circle-translate_         | l z       | yes
|_circle-translate-anchor_  |           | no