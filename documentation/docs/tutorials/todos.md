# Todo format description

Vespucci Todos are stored in a simple JSON format similar to the Osmose API output.

## Example

    {
        "name": "Documentation",
        "todos": [
            {
                "lat": 47.4265935,
                "lon": 8.3804539,
                "id": "819fa191-4348-4f95-b04c-d84212f43c55",
                "state": "OPEN",
                "comment": "This is a way",
                "osm_ids": {
                    "ways": [
                        1077196514
                    ]
                }
            },
            {
                "lat": 47.426666,
                "lon": 8.3799188,
                "id": "d937c08d-0870-4963-b58d-8dd8fa9dd08d",
                "state": "OPEN",
                "comment": "This is a node",
                "osm_ids": {
                    "nodes": [
                        1329455443
                    ]
                }
            },
            {
                "lat": 47.4226375,
                "lon": 8.3702006,
                "id": "b39ae2e3-3ba2-4665-9b1d-7bf46a3d2257",
                "state": "OPEN",
                "comment": "This is a relation",
                "osm_ids": {
                    "relations": [
                        5990133
                    ]
                }
            }
        ]
    }


## Schema

_Provisional and as per May 16th 2023_

    {
        "$schema": "http://json-schema.org/draft-04/schema#",
        "type": "object",
        "properties": {
            "name": {
                "description": "The name of the todo list",
                "type": "string"
            },
            "todos": {
                "description": "Array of the todos",
                "type": "array",
                "items": [
                    {
                        "type": "object",
                        "properties": {
                            "lat": {
                                "description": "WGS84 latitude in decimal degrees",
                                "type": "number"
                            },
                            "lon": {
                                "description": "WGS84 longitude in decimal degrees",
                                "type": "number"
                            },
                            "id": {
                                "description": "UUID for the todo",
                                "type": "string"
                            },
                            "state": {
                                "description": "State of  the todo",
                                "type": "string",
                                "enum": [
                                    "OPEN",
                                    "CLOSED"
                                ]
                            },
                            "comment": {
                                "description": "Per todo comment",
                                "type": "string"
                            },
                            "osm_ids": {
                                "type": "object",
                                "properties": {
                                    "nodes": {
                                        "type": "array",
                                        "items": [
                                            {
                                                "type": "integer"
                                            }
                                        ]
                                    },
                                    "ways": {
                                        "type": "array",
                                        "items": [
                                            {
                                                "type": "integer"
                                            }
                                        ]
                                    },
                                    "relations": {
                                        "type": "array",
                                        "items": [
                                            {
                                                "type": "integer"
                                            }
                                        ]
                                    }
                                },
                                "additionalProperties": false
                            }
                        },
                        "required": [
                            "lat",
                            "lon",
                            "id",
                            "state"
                        ]
                    }
                ]
            }
        },
        "required": [
            "name",
            "todos"
        ]
    }
