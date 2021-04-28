const express = require("express");
const mysql = require("mysql");
var bodyParser = require('body-parser')
const fetch = require("node-fetch");
const app = express();

const timeout = 60;

app.use(function (req, res, next) {
  res.header("Access-Control-Allow-Origin", "http://35.246.98.225:3000");
  res.header("Access-Control-Allow-Headers", "Origin, X-Requested-With, Content-Type, Accept");
  res.header('Access-Control-Allow-Methods', 'PUT, POST, GET, DELETE, OPTIONS');
  //res.header("Access-Control-Allow-Origin", "http://azkeryon.duckdns.org:3000");
  next();
});

app.use(bodyParser.urlencoded())
app.use(bodyParser.json())

var mCounter = {
  counter: 0,

  getCount: function() {
    return this.counter;
  },

  addCount: function() {
    return this.counter ++;
  },

  reset: function() {
    this.counter = 0;
  }
};

const db = mysql.createConnection({
  host: "34.89.43.64",
  user: "pti",
  password: "wesuck",
  database: "Maps"
});


db.connect(err => {
  if (err) throw err;
  console.log("Connected to MySQL");
  /*let query = "select * from Locality";
  db.query(query, (err, result) => {
    console.log(result);
  });*/
});

app.get("/", (req, res) => {
  res.send("ok").status(200);
});

app.get("/login/:email/:password", (req, res) => {
  let email = req.params.email;
  let password = req.params.password;

  console.log("New login from " + email);

  let query = "select U.id, U.email, U.name, U.typeId, U.latitude, U.longitude, U.altitude, U.speed, U.bearing, U.status, U.sos "
    + "from User U where email = '" + email + "' and password = '" + password + "'";

  db.query(query, (err, result) => {
    if (err) {
      res.send(err).status(400);
    }

    try {
      if (result[0].id) {
        res.send(result[0]).status(200);
      }
    }

    catch (Exception) {
      let idJson = JSON.stringify ({
        id: -1
      });

      res.send(idJson).status(200);
    }
  })
})

app.put("/data/:id", (req, res) => {  
  let id = parseInt(req.params.id);
  let latitude = req.body.latitude;
  let longitude = req.body.longitude;
  let altitude = req.body.altitude;
  let speed = req.body.speed;
  let bearing = req.body.bearing;
  var route = req.body.route;
  var locality = req.body.locality;
  var speedLimit = req.body.speedLimit;

  console.log("PUT/data/" + id + " received");

  //console.log("[PUTTED] " + id + " " + latitude + " " + longitude + " " + altitude + " " + speed + " " + bearing + " " + route + " " + locality + " " + speedLimit);

  let selectLocality = "select id from Locality where name = '" + locality + "'";
  var localityId = -1;

  db.query(selectLocality, (err, result) => {
    try {
      //console.log("localoty result =>"+result)
      if (result[0].id) {
        localityId = result[0].id;
        routeQuery(localityId);
      }
    }

    catch (e) {
      let insertLocality = "insert into Locality(name) VALUES('" + locality + "')";
      //console.log("QUERY " + insertLocality);
      db.query(insertLocality, (err, result) => {
        db.query(selectLocality, (err, result) => {
          //console.log("locality result => " + result)
          if (result[0].id) {
            localityId = result[0].id;
            routeQuery(localityId);
          }
        });
      });
    }
  })

  routeQuery = (localityId) => {
    //console.log("LOCALITY " + localityId);

    let selectRoute = "select id from Route where name = '" + route + "' and localityId = " + localityId;
    //console.log("QUERY " + selectRoute);
    var routeId = -1;

    db.query(selectRoute, (err, result) => {
      try {
        if (result[0].id) {
          routeId = result[0].id;
          userQuery(routeId);
        }
      }

      catch (e) {
        let insertRoute = "insert into Route(name, localityId) VALUES('" + route + "', " + localityId + ")";
      //  console.log("QUERY " + insertRoute);
        db.query(insertRoute, (err, result) => {
          db.query(selectRoute, (err, result) => {
            if (result[0].id) {
              routeId = result[0].id;
              userQuery(routeId);
              
            }
          });
        });
      }
    })   
  }

  userQuery = (routeId) => {
    //console.log("ROUTE " + routeId);

    let query = "update User SET latitude = " + latitude + ", longitude = " + longitude
      + ", altitude = " + altitude + ", speed = " + speed + " , bearing = " + bearing
      + ", speedLimit = " + speedLimit + ", routeId = " + routeId
      + " where id = " + id;
      
    //console.log("QUERY " + query);
    
    db.query(query, (err, result) => {
      if (err) {
        res.send(err).status(400);
      }
    })
  }     
});

app.put("/sos/:id", (req, res) => {
  let id = parseInt(req.params.id);
  let sos = req.body.sos;

  let query = "update User SET sos = " + sos + " where id = " + id;

  console.log("PUT/sos/" + id + " received -> setting SOS to " + sos);
    
  db.query(query, (err, result) => {
    if (err) {
      res.send(err).status(400);
    }

    else {
      res.send(JSON.stringify({id, sos}));
    }
  })
});

app.put("/status/:id", (req, res) => {
  let id = parseInt(req.params.id);
  let status = req.body.status;

  let query = "update User SET status = " + status + " where id = " + id;

  console.log("PUT/status/" + id + " received -> setting status to " + status);
    
  db.query(query, (err, result) => {
    if (err) {
      res.send(err).status(400);
    } else {
      res.send(JSON.stringify({id, status}));
    }
  })
});

app.get("/users/:id", (req, res) => {
  let id = parseInt(req.params.id);

  let authQuery = "select U.authLocalityId from User U where U.id = " + id;

  db.query(authQuery, (err, result) => {
    let query = "select U.id, U.name, U.typeId from User U where U.authLocalityId = " + result[0].authLocalityId;
  
    db.query(query, (err, result2) => {
      let userList = {
        users: result2
      }

      console.log("GET/users/" + id + " received -> responding " + JSON.stringify(userList));
      
      res.send(JSON.stringify(userList));
    });
  });  
});

app.put("/types/:id", (req, res) => {
  let id = parseInt(req.params.id);
  let typeId = req.body.typeId;

  let updateQuery = "update User SET typeId = " + typeId + ", status = 0 where id = " + id;
  console.log("PUT/types/" + id + " received -> changing type to " + typeId);

  db.query(updateQuery, (err, result2) => {
    if (err) {
      res.send(err).status(400);
    }

    else {
      res.status(200).json({status: "ok"})
    }
  }); 
});

//var tester = 0;
//var tester2 = 0;
var isOk = true;
var trafficJson;

app.get("/traffic/:segments", (req, res) => {
  isOk = false;
  //tester ++;
  //console.log("GOT SEGMENTS ", req.params.segments);
  let segments = JSON.parse(req.params.segments);

  //console.log("GOT SEGMENTS JSON ", segments);
  var trafficDelay = 0;
  var counter = 0;

  let total = segments.length;

  var latitude = [];
  var longitude = [];
  var routeLength = [];
  var routeDuration = [];
  var nVehicles = [];
  var traffic = [];
  var routeNames = [];
  var routeIds = [];
  var speedLimit = [];
  var coordinates = "";

  console.log("GET/traffic/" + req.params.segments + " received");

  segments.forEach((segment, i) => {
    latitude[i] = segment.latitude;
    longitude[i] = segment.longitude;
    routeLength[i] = segment.routeLength;
    routeDuration[i] = segment.routeDuration;

    if (i == 0) {
      coordinates = latitude[i] + "," + longitude[i];
    }

    else {
      coordinates += "|" + latitude[i] + "," + longitude[i];
    }
  });
  
  fetch("https://roads.googleapis.com/v1/speedLimits?path=" + coordinates + "&key=AIzaSyADYWIGFSnn3DHlJblK0hntz5KQiwbD0hk")
  .then(response => response.json())
  .then(json => {
    json.speedLimits.forEach((limit, i) => {
      speedLimit[i] = limit.speedLimit;
    });

    //console.log(tester + ". speedLimits");

    //console.log("SPEED LIMITS " + speedLimit);
    //tester2 = 0;
    geocoder();
  });

  geocoder = () => {
    fetch("https://maps.googleapis.com/maps/api/geocode/json?latlng=" + latitude[counter] + "," + longitude[counter] + "&key=AIzaSyADYWIGFSnn3DHlJblK0hntz5KQiwbD0hk")
    .then(response => response.json()).then(json => {
      //tester2 ++;

      //console.log(tester + "." + tester2 + ". geocode");
      
      var locality = "Unknown";
      var route = "Unknown";

      var gotRoute = false;
      var gotLocality = false;
      var results = json.results;

      for (var i = 0; i < results.length; i ++) {
        var address_components = results[i].address_components;

        for (var j = 0; j < address_components.length; j ++) {
          var type = address_components[j].types[0];
          
          if (!gotRoute && type != "street_number" && type != "route") {
            break;
          }

          if (type == "route") {
            route = address_components[j].short_name;
            //console.log("ROUTE " + route);
            gotRoute = true;
          }

          if (gotRoute) {
            if (type == "locality" || type == "sublocality" || type == "neighborhood") {
              locality = address_components[j].short_name;
              //console.log("LOCALITY " + locality);
              gotLocality = true;

              break;
            }
          }
        }

        if (!gotRoute || !gotLocality) {
          route = "Unknown";
          locality = "Unknown";
          gotRoute = false;
          gotLocality = false;
        }

        else {
            break;
        }
      }

      //console.log(tester + "." + tester2 + ". geocode2");
      
      //console.log("GOOGLE RESPONSES " + route + " " + locality )

      let selectLocality = "select id from Locality where name = '" + locality + "'";
      //console.log("LOCALITY QUERY " + selectLocality);
      var localityId = -1;

      db.query(selectLocality, (err, result) => {
        try {
          //console.log(tester + "." + tester2 + "localoty try result:");
          //console.log(result);

          if (result[0].id) {
            localityId = result[0].id;
            //console.log(tester + "." + tester2 + "I SHOULD BE HERE (" + localityId + ")");
            routeQuery(localityId);
          }
          //console.log(tester + "." + tester2 + "I SHOULDN'T BE HERE");
          if (!isOk) {
            console.log("GET/traffic fixing -> " + JSON.stringify(trafficJson));
            try {
              res.send(JSON.stringify(trafficJson));
            } catch (error) {
              console.log('RIP ' + error);
            }
          }
        }

        catch (e) {
          let insertLocality = "insert into Locality(name) VALUES('" + locality + "')";
          //console.log(tester + "." + tester2 + "LOCALITY QUERY2 " + insertLocality);
          db.query(insertLocality, (err, result) => {
            db.query(selectLocality, (err, result) => {
              //console.log("locality result => " + result)
              if (result[0].id) {
                localityId = result[0].id;
                routeQuery(localityId);
              }
            });
          });
        }
      });

      routeQuery = (localityId) => {
        //console.log(tester + "." + tester2 + ". locality " + localityId);
        isOk = true;
        //console.log("LOCALITY RESULT " + localityId);
        routeNames[counter] = route;
        let selectRoute = "select id from Route where name = '" + route + "' and localityId = " + localityId;
        //console.log("ROUTE QUERY " + selectRoute);
        var routeId = -1;

        db.query(selectRoute, (err, result) => {
          try {
            if (result[0].id) {
              routeId = result[0].id;
              getTraffic(routeId);
            }
          }

          catch (e) {
            let insertRoute = "insert into Route(name, localityId) VALUES('" + route + "', " + localityId + ")";
            //console.log("ROUTE QUERY " + insertRoute);
            db.query(insertRoute, (err, result) => {
              db.query(selectRoute, (err, result) => {
                if (result[0].id) {
                  routeId = result[0].id;
                  getTraffic(routeId);
                }
              });
            });
          }
        });
      }

      getTraffic = (routeId) => {
        //console.log(tester + "." + tester2 + ". route " + routeId);
        //console.log("ROUTE RESULT " + routeId);
        
        //console.log("COUTNER " + counter + " | LIMIT " + speedLimit[counter] + " | TOTAL " + total);

        if (routeId != null) {
          routeIds[counter] = routeId;

          let nearbyQuery = "select count(*) as count from User "
            + "where routeId = " + routeId + " and speed < " + speedLimit[counter] * 0.8 + " "
            + "and lastTimestamp >= CURRENT_TIMESTAMP - " + timeout;
          //console.log("NEARBY QUERY " + nearbyQuery);

          db.query(nearbyQuery, (err, result) => {
            //console.log(tester + "." + tester2 + ". traffic");
            if (err) {
              //console.log("nearby err " + err);
            }

            let c;

            if (result == null) {
              c = 0;
              nVehicles[counter] = 0;
            }

            else {
              c = parseInt(result[0].count) * 50 / routeLength[counter];
              nVehicles[counter] = parseInt(result[0].count);
            }
  
            //console.log("NEARBY RESULT " + result[0].count);

            

            if (c <= 4) {
              traffic[counter] = 0;
            }

            else if (c > 4 && c <= 9) {
              traffic[counter] = 1;
              trafficDelay += routeDuration[counter] * 0.5;
            }

            else {
              traffic[counter] = 2;
              trafficDelay += routeDuration[counter] * (c / 10);
            }

            //console.log('CCCCCCCC: ' + c + ' ' + nVehicles[counter] + ' ' + traffic[counter]);

            //console.log("c = " + c + ", traffic = " + traffic);
            
            if (counter < total) {
              counter ++;
              /*setTimeout(function () {
                geocoder();
              }, 100);*/
              geocoder();
            }

            else {
              trafficJson = {
                delay: trafficDelay,
                traffic: traffic,
                nVehicles: nVehicles,
                routeLength: routeLength,
                routeIds: routeIds,
                speedLimit: speedLimit
              };
    
              //console.log(trafficJson);
              console.log("GET/traffic responding -> " + JSON.stringify(trafficJson));
              try {
                res.send(JSON.stringify(trafficJson));
              } catch (error) {
                console.log('RIP ' + error);
              }
            }
          })
        }

        else {
          //console.log(tester + "." + tester2 + ". null traffic");
          traffic[counter] = traffic[counter - 1];
          nVehicles[counter] = nVehicles[counter - 1];
          routeLength[counter] = routeLength[counter - 1];
          routeIds[counter] = routeIds[counter - 1];
          speedLimit[counter] = speedLimit[counter - 1];

          //console.log("c = idk, traffic = " + traffic);

          trafficJson = {
            delay: trafficDelay,
            traffic: traffic,
            nVehicles: nVehicles,
            routeLength: routeLength,
            routeIds: routeIds,
            speedLimit: speedLimit
          };

          //console.log(trafficJson);
          console.log("GET/traffic responding -> " + JSON.stringify(trafficJson));
          try {
            res.send(JSON.stringify(trafficJson));
          } catch (error) {
            console.log('RIP ' + error);
          }
        }
      }
    })
  }
  
});

app.get("/data/:id", (req, res) => {
  let id = req.params.id;
  console.log("GET/data/" + id + " receiving");
  let dataQuery = "select U.typeId, U.latitude, U.longitude, U.altitude, U.speed, U.bearing, U.speedLimit,"
    + " UNIX_TIMESTAMP(U.lastTimestamp) as lastTimestamp, R.id as routeId, R.name as routeName,"
    + " L.id as localityId, L.name as localityName, U.authLocalityId"
    + " from User U, Route R, Locality L where U.id = " + id + " and R.id = U.routeId and L.id = R.localityId"

  db.query(dataQuery, (err, result) => {
    //console.log("dataQuery:")
    //console.log(result);

    let typeId = result[0].typeId;
    let latitude = result[0].latitude;
    let longitude = result[0].longitude;
    let altitude = result[0].altitude;
    let speed = result[0].speed;
    let bearing = result[0].bearing;
    let speedLimit = result[0].speedLimit;
    let lastTimestamp = result[0].lastTimestamp;
    let routeId = result[0].routeId;
    let routeName = result[0].routeName;
    let localityId = result[0].localityId;
    let localityName = result[0].localityName;
    let authLocalityId = result[0].authLocalityId;

    let myJson = {
      latitude,
      longitude,
      altitude,
      speed,
      bearing,
      speedLimit,
      lastTimestamp,
      routeName,
      localityName
    }

    if (speed < speedLimit * 0.8 /*&& lastTimestamp >= Math.round((new Date()).getTime() / 1000) - timeout*/) {
      let nearbyQuery = "select latitude, longitude from User"
        + " where routeId = " + routeId + " and speed < " + speedLimit * 0.8
        + " and lastTimestamp >= CURRENT_TIMESTAMP - " + timeout;
      var c = 0;
      
      db.query(nearbyQuery, (err, result) => {
        result.forEach(nResult => {
          let s = speed > 50 ? speed : 50;
          let φ = latitude / (180 / Math.PI);
          let θ = longitude / (180 / Math.PI);
          let φi = nResult.latitude / (180 / Math.PI);
          let θi = nResult.longitude / (180 / Math.PI);
          let R = 6371e3;

          let d = Math.acos(Math.sin(φ) * Math.sin(φi) + Math.cos(φ) * Math.cos(φi) * Math.cos(θi - θ)) * R;

          if (d < s) {
            c ++;
          }
        });

        if (c <= 4) {
          traffic = 0;
        }

        else if (c > 4 && c <= 9) {
          traffic = 1;
        }

        else {
          traffic = 2;
        }
        
        console.log("c = " + c);

        typesQuery(myJson, traffic, localityId, authLocalityId, typeId);
      });
    }

    else {
      console.log("c = none");
      typesQuery(myJson, 0, localityId, authLocalityId, typeId);
    }
  });

  typesQuery = (myJson, traffic, localityId, authLocalityId, typeId) => {
    const {
      latitude,
      longitude,
      altitude,
      speed,
      bearing,
      speedLimit,
      lastTimestamp,
      routeName,
      localityName
    } = myJson;

    let selectOut = "select U.id, U.name, U.typeId, U.latitude, U.longitude, U.speed, U.bearing, U.speedLimit, U.status, U.sos"

    //console.log("switching " + typeId);

    var selectVehicles;

    switch (typeId) {   
      case 1: //Padrão
        selectVehicles = selectOut + " from User U, Route R"
          + " where U.status >= 1 and UNIX_TIMESTAMP(U.lastTimestamp) >= UNIX_TIMESTAMP(CURRENT_TIMESTAMP()) - " + timeout
          + " and R.id = U.routeId and R.localityId = " + localityId + " and U.id != " + id;

        db.query(selectVehicles, (err, result) => {
          //console.log("selectTypes:")
          //console.log(result);
    
          let vehicles = [];
    
          result.forEach((nResult, i) => {
            vehicles[i] = {
              id: nResult.id,
              name: nResult.name,
              typeId: nResult.typeId,
              latitude: nResult.latitude,
              longitude: nResult.longitude,
              speed: nResult.speed,
              bearing: nResult.bearing,
              speedLimit: nResult.speedLimit,
              status: nResult.status,
              sos: nResult.sos
            };
          });

          let dataJson = JSON.stringify ({
            latitude: latitude,
            longitude: longitude,
            altitude: altitude,
            speed: speed,
            bearing: bearing,
            speedLimit: speedLimit,
            lastTimestamp: lastTimestamp,
            routeName: routeName,
            localityName: localityName,
            traffic: traffic,
            vehicles: vehicles
          });

          console.log("GET/data/" + id + " responding -> " + latitude + "º, " + longitude + "º, " + altitude + " m, "
            + speed + " km/h (" + speedLimit + "), " + bearing + "º, " + routeName + ", " + localityName + ", t"
            + traffic + ", v" + vehicles.length);
          res.send(dataJson);
        });

        break;

      case 2: //Autoritário
        selectVehicles = selectOut + " from User U, Route R"
          + " where ((U.status > 0 and UNIX_TIMESTAMP(U.lastTimestamp) >= UNIX_TIMESTAMP(CURRENT_TIMESTAMP()) - " + timeout
          + " and R.id = U.routeId and R.localityId = " + localityId + ")"
          + " or (UNIX_TIMESTAMP(U.lastTimestamp) >= UNIX_TIMESTAMP(CURRENT_TIMESTAMP()) - " + timeout
          + " and R.id = U.routeId and R.localityId = " + authLocalityId + ")) and U.id != " + id;

        //console.log(selectVehicles);

        db.query(selectVehicles, (err, result) => {
          //console.log("selectTypes:")
          //console.log(result);
    
          let vehicles = [];
    
          result.forEach((nResult, i) => {
            vehicles[i] = {
              id: nResult.id,
              name: nResult.name,
              typeId: nResult.typeId,
              latitude: nResult.latitude,
              longitude: nResult.longitude,
              speed: nResult.speed,
              bearing: nResult.bearing,
              speedLimit: nResult.speedLimit,
              status: nResult.status,
              sos: nResult.sos
            };
          });

          let dataJson = JSON.stringify ({
            latitude: latitude,
            longitude: longitude,
            altitude: altitude,
            speed: speed,
            bearing: bearing,
            speedLimit: speedLimit,
            lastTimestamp: lastTimestamp,
            routeName: routeName,
            localityName: localityName,
            traffic: traffic,
            vehicles: vehicles
          });
          //console.log(dataJson);
          console.log("GET/data/" + id + " responding -> " + latitude + "º, " + longitude + "º, " + altitude + " m, "
            + speed + " km/h (" + speedLimit + "), " + bearing + "º, " + routeName + ", " + localityName + ", t"
            + traffic + ", v" + vehicles.length);
          res.send(dataJson);
        });
        
        break;

      case 3: //Táxi
        selectVehicles = selectOut + " from User U, Route R"
          + " where (U.status > 0 and UNIX_TIMESTAMP(U.lastTimestamp) >= UNIX_TIMESTAMP(CURRENT_TIMESTAMP()) - " + timeout
          + " and R.id = U.routeId and R.localityId = " + localityId + ") and U.id != " + id;

        db.query(selectVehicles, (err, result) => {
          //console.log("selectTypes:")
          //console.log(result);
    
          let vehicles = [];
    
          result.forEach((nResult, i) => {
            vehicles[i] = {
              id: nResult.id,
              name: nResult.name,
              typeId: nResult.typeId,
              latitude: nResult.latitude,
              longitude: nResult.longitude,
              speed: nResult.speed,
              bearing: nResult.bearing,
              speedLimit: nResult.speedLimit,
              status: nResult.status,
              sos: nResult.sos
            };
          });

          let dataJson = JSON.stringify ({
            latitude: latitude,
            longitude: longitude,
            altitude: altitude,
            speed: speed,
            bearing: bearing,
            speedLimit: speedLimit,
            lastTimestamp: lastTimestamp,
            routeName: routeName,
            localityName: localityName,
            traffic: traffic,
            vehicles: vehicles
          });

          //console.log(dataJson);
          console.log("GET/data/" + id + " responding -> " + latitude + "º, " + longitude + "º, " + altitude + " m, "
            + speed + " km/h (" + speedLimit + "), " + bearing + "º, " + routeName + ", " + localityName + ", t"
            + traffic + ", v" + vehicles.length);
          res.send(dataJson);
        });

        break;

      case 4: //Autocarro
        selectVehicles = selectOut + " from User U, Route R"
          + " where (U.status > 0 and UNIX_TIMESTAMP(U.lastTimestamp) >= UNIX_TIMESTAMP(CURRENT_TIMESTAMP()) - " + timeout
          + " and R.id = U.routeId and R.localityId = " + localityId + ") and U.id != " + id;

        db.query(selectVehicles, (err, result) => {
          //console.log("selectTypes:")
          //console.log(result);
    
          let vehicles = [];
    
          result.forEach((nResult, i) => {
            vehicles[i] = {
              id: nResult.id,
              name: nResult.name,
              typeId: nResult.typeId,
              latitude: nResult.latitude,
              longitude: nResult.longitude,
              speed: nResult.speed,
              bearing: nResult.bearing,
              speedLimit: nResult.speedLimit,
              status: nResult.status,
              sos: nResult.sos
            };
          });

          let dataJson = JSON.stringify ({
            latitude: latitude,
            longitude: longitude,
            altitude: altitude,
            speed: speed,
            bearing: bearing,
            speedLimit: speedLimit,
            lastTimestamp: lastTimestamp,
            routeName: routeName,
            localityName: localityName,
            traffic: traffic,
            vehicles: vehicles
          });

          //console.log(dataJson);
          console.log("GET/data/" + id + " responding -> " + latitude + "º, " + longitude + "º, " + altitude + " m, "
            + speed + " km/h (" + speedLimit + "), " + bearing + "º, " + routeName + ", " + localityName + ", t"
            + traffic + ", v" + vehicles.length);
          res.send(dataJson);
        });

        break;

      case 5: //Ambulância
        selectVehicles = selectOut + " from User U, Route R"
          + " where ((U.status > 0 and UNIX_TIMESTAMP(U.lastTimestamp) >= UNIX_TIMESTAMP(CURRENT_TIMESTAMP()) - " + timeout
          + " and R.id = U.routeId and R.localityId = " + localityId + ")"
          + " or (U.sos = 5 and UNIX_TIMESTAMP(U.lastTimestamp) >= UNIX_TIMESTAMP(CURRENT_TIMESTAMP()) - " + timeout
          + " and R.id = U.routeId and R.localityId = " + authLocalityId + ")) and U.id != " + id;

        db.query(selectVehicles, (err, result) => {
          //console.log("selectTypes:")
          //console.log(result);
    
          let vehicles = [];
    
          result.forEach((nResult, i) => {
            vehicles[i] = {
              id: nResult.id,
              name: nResult.name,
              typeId: nResult.typeId,
              latitude: nResult.latitude,
              longitude: nResult.longitude,
              speed: nResult.speed,
              bearing: nResult.bearing,
              speedLimit: nResult.speedLimit,
              status: nResult.status,
              sos: nResult.sos
            };
          });

          let dataJson = JSON.stringify ({
            latitude: latitude,
            longitude: longitude,
            altitude: altitude,
            speed: speed,
            bearing: bearing,
            speedLimit: speedLimit,
            lastTimestamp: lastTimestamp,
            routeName: routeName,
            localityName: localityName,
            traffic: traffic,
            vehicles: vehicles
          });

          //console.log(dataJson);
          console.log("GET/data/" + id + " responding -> " + latitude + "º, " + longitude + "º, " + altitude + " m, "
            + speed + " km/h (" + speedLimit + "), " + bearing + "º, " + routeName + ", " + localityName + ", t"
            + traffic + ", v" + vehicles.length);
          res.send(dataJson);
        });

        break;

      case 6: //Polícia
        selectVehicles = selectOut + " from User U, Route R"
          + " where ((U.status > 0 and UNIX_TIMESTAMP(U.lastTimestamp) >= UNIX_TIMESTAMP(CURRENT_TIMESTAMP()) - " + timeout
          + " and R.id = U.routeId and R.localityId = " + localityId + ")"
          + " or (U.sos = 6 and UNIX_TIMESTAMP(U.lastTimestamp) >= UNIX_TIMESTAMP(CURRENT_TIMESTAMP()) - " + timeout
          + " and R.id = U.routeId and R.localityId = " + authLocalityId + ")"
          + " or (U.speed > U.speedLimit + 30 and UNIX_TIMESTAMP(U.lastTimestamp) >= UNIX_TIMESTAMP(CURRENT_TIMESTAMP()) - " + timeout
          + " and R.id = U.routeId and R.localityId = " + authLocalityId + ")) and U.id != " + id;

        db.query(selectVehicles, (err, result) => {
          //console.log("selectTypes:")
          //console.log(result);
    
          let vehicles = [];
    
          result.forEach((nResult, i) => {
            vehicles[i] = {
              id: nResult.id,
              name: nResult.name,
              typeId: nResult.typeId,
              latitude: nResult.latitude,
              longitude: nResult.longitude,
              speed: nResult.speed,
              bearing: nResult.bearing,
              speedLimit: nResult.speedLimit,
              status: nResult.status,
              sos: nResult.sos
            };
          });

          let dataJson = JSON.stringify ({
            latitude: latitude,
            longitude: longitude,
            altitude: altitude,
            speed: speed,
            bearing: bearing,
            speedLimit: speedLimit,
            lastTimestamp: lastTimestamp,
            routeName: routeName,
            localityName: localityName,
            traffic: traffic,
            vehicles: vehicles
          });

          //console.log(dataJson);
          console.log("GET/data/" + id + " responding -> " + latitude + "º, " + longitude + "º, " + altitude + " m, "
            + speed + " km/h (" + speedLimit + "), " + bearing + "º, " + routeName + ", " + localityName + ", t"
            + traffic + ", v" + vehicles.length);
          res.send(dataJson);
        });

        break;

      case 7: //Reboque
        selectVehicles = selectOut + " from User U, Route R"
          + " where ((U.status > 0 and UNIX_TIMESTAMP(U.lastTimestamp) >= UNIX_TIMESTAMP(CURRENT_TIMESTAMP()) - " + timeout
          + " and R.id = U.routeId and R.localityId = " + localityId + ")"
          + " or (U.sos = 7 and UNIX_TIMESTAMP(U.lastTimestamp) >= UNIX_TIMESTAMP(CURRENT_TIMESTAMP()) - " + timeout
          + " and R.id = U.routeId and R.localityId = " + authLocalityId + ")) and U.id != " + id;

        db.query(selectVehicles, (err, result) => {
          //console.log("selectTypes:")
          //console.log(result);
    
          let vehicles = [];
    
          result.forEach((nResult, i) => {
            vehicles[i] = {
              id: nResult.id,
              name: nResult.name,
              typeId: nResult.typeId,
              latitude: nResult.latitude,
              longitude: nResult.longitude,
              speed: nResult.speed,
              bearing: nResult.bearing,
              speedLimit: nResult.speedLimit,
              status: nResult.status,
              sos: nResult.sos
            };
          });

          let dataJson = JSON.stringify ({
            latitude: latitude,
            longitude: longitude,
            altitude: altitude,
            speed: speed,
            bearing: bearing,
            speedLimit: speedLimit,
            lastTimestamp: lastTimestamp,
            routeName: routeName,
            localityName: localityName,
            traffic: traffic,
            vehicles: vehicles
          });

          //console.log(dataJson);
          console.log("GET/data/" + id + " responding -> " + latitude + "º, " + longitude + "º, " + altitude + " m, "
            + speed + " km/h (" + speedLimit + "), " + bearing + "º, " + routeName + ", " + localityName + ", t"
            + traffic + ", v" + vehicles.length);
          res.send(dataJson);
        });

        break;

      default:
        break;
    }

      //console.log("post switching " + typeId);
  }
})

app.post("/signup", (req, res) => {
  let email = req.body.email;
  let name = req.body.name;
  let password = req.body.password;
  let locality = req.body.locality;

  locality = (locality.charAt(0).toUpperCase() + locality.substring(1)).normalize("NFD").replace(/[\u0300-\u036f]/g, "");

  console.log("POST/signup receiving -> " + email + ", " + password + ", " + name + ", " + locality);

  fetch("https://maps.googleapis.com/maps/api/geocode/json?address=" + locality + "&key=AIzaSyADYWIGFSnn3DHlJblK0hntz5KQiwbD0hk")
    .then(response => response.json()).then(json => {
    console.log(json);
    let latitude = json.results[0].geometry.location.lat;
    let longitude = json.results[0].geometry.location.lng;

    let selectLocality = "select id from Locality where name = '" + locality + "'";
    let authLocalityId = -1;
    console.log(">> selectLocality | " + selectLocality);

    db.query(selectLocality, (err, result) => {
      try {
        if (result[0].id) {
          authLocalityId = result[0].id;
          userQuery(authLocalityId);
        }
      }

      catch (e) {
        let insertLocality = "insert into Locality(name) VALUES('" + locality + "')";
        console.log(">> insertLocality | " + insertLocality);
        db.query(insertLocality, (err, result) => {
          console.log(">> selectLocality2 | " + selectLocality);
          db.query(selectLocality, (err, result) => {
            if (result[0].id) {
              authLocalityId = result[0].id;
              userQuery(authLocalityId);
            }
          });
        });
      }
    });

    userQuery = (authLocalityId) => {
      let query = "insert into User(email, name, password, authLocalityId, latitude, longitude, routeId) values('" + email + "', '" + name + "', '" + password + "', " + authLocalityId + ", " + latitude + ", " + longitude + ", 1)";
      console.log(">> query | " + query);
      db.query(query, (err, result) => {
        console.log("POST/signup -> updated " + email + ", " + password + ", " + name + ", " + authLocalityId + ", " + latitude + ", " + longitude);
        if (err) res.send(err).status(400);
        res.send("ok").status(200);

      });
    }
  });  
});

app.listen(8000, () => {
  console.log("Listening on 8000");
});
