# GPS Sources

**(this is currently an experimental facility)**

Besides the Android standard Location mechanism, Vespucci supports three other methods of obtaining location information:

* the internal NMEA source
* NEMA sentences retrieved from a server
* NEMA sentences sent from a client to a built-in Vespucci server

The main purpose of this is (internal NMEA being more of an academic exercise) to enable location updates from an external or on device RTKLIB instance. All three sources are unfiltered and the minimum GPS change values are ignored.

In all three cases the only sentences that are supported are $..GGA and $..GNS. Vespucci will prioritize information from NMEA "talkers" as follows: GN (multiple talkers) > GP (GPS) > GL (GLONASS).

### Setup

In the "Advanced preferences" you can select the "GPS source"

* **Internal** - default
* **Internal NMEA**
* **NMEA from TCP client** - use a client to retrieve NMEA data from an external server
* **NMEA from TCP server** - allow an external client to submit NMEA data

For the later two options you need to set the "NMEA network source", the string has the format **host:port**. The host is ignored for the server option and the process binds to the external address of the device (not localhost) at the configured port.