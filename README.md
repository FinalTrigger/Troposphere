# Troposphere
Weather app for CS656

Based on the Forecast.io API to GET JSON data for a location (currently hardcoded).

![alt text](https://raw.github.com/finaltrigger/troposphere/master/TropoSphere_Preview_Image.png)


# Key Releases

1/24:
- GET JSON data from forecast.io and log data into console
- Ability to verify network access and connectivity (Dialog + Toast alerts for network issues).

1/25:
- Added weather UI drawables to project
- Created new class CurrentWeather to parse JSON data properly
- Filter weather data to associated drawable
- Add Object to convert UNIX system time to standard date time "h:mm am/pm"

2/2:
- Finished parsing all JSON data and setting them to the correct variables
- Implement ButterKnife API to reduce boiler code on declaring/setting variables
- Update the text holders on the view with the relative data
- Completed UI
    Weather
    Humidity
    Precipitation
    Summary
    Weather Icon
    Current Time
    Refresh / Progress loader
- Application works currently with pre defined location set to Newark, NJ (NJIT)
- Refactor entire code for memory optimization and readability flow w/ comments

###### Note:
- The app is complete. All additional features are to be added such as weather data based on user location via GPS social media info share functionality
