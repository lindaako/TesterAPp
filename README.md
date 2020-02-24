# TesterAPp
(Bluetooth auto-pair enabled)

Android application in order to search for books in a library's database/website, get the location of the book and send the location data to a navigation robot via bluetooth.
The application auto-pairs to the robot and sends the location data. If the data is sent successfully, the auto-forget protocal starts and the mobile phone is disconnected and forgotten from the list of paired devices in the RaspberryPi. 
If the data is not sent properly then the RaspberryPi is programmed to wait until the correct data is sent.
