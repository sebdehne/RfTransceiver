'use strict';


angular.module('project.services', [])

  // A simple key-value store used to communicate data between controllers
  .factory('KeyValueStore', function () {
    var store = {};
    return {
      get: function (key) {
        return store[key];
      },
      put: function (key, value) {
        store[key] = value;
      },
      remove: function (key) {
        if (store[key]) {
          var temp = store[key];
          delete store[key];
          return temp;
        } else {
          return undefined;
        }
      }
    };
  })

  .factory('ErrorService', function () {
    return {
      show: function (str) {
        $('#errorMsg').text(str);
        $('#errorMsg').show();
      },
      hide: function () {
        $('#errorMsg').hide();
      }
    };
  })

  .factory('HttpWrapper', function (ErrorService) {
    return {
      handleResult: function (http, errorStr, fn, errorFn) {
        $('#workingMsg').show();
        http
          .success(function (data, status, headers, config) {
            $('#workingMsg').hide();
            if (fn) {
              fn(data, status, headers, config);
            }
          })
          .error(function (data, status, headers, config) {
            $('#workingMsg').hide();
            if (errorStr) {
              ErrorService.show(errorStr + ': ' + data.response);
            }
            if (errorFn) {
              errorFn(data, status, headers, config);
            }
          });
      }
    };
  })

  .factory('GarageService', function ($http, HttpWrapper) {
    return {
      getStatus: function (fn) {
        HttpWrapper.handleResult($http.get('/garage_door'), 'Could not fetch the garage status', fn);
      },
      sendClose: function (fn) {
        HttpWrapper.handleResult(
          $http.post('/garage_door/action?command=close'),
          'Could not send close command',
          fn);
      },
      sendOpen: function (fn) {
        HttpWrapper.handleResult(
          $http.post('/garage_door/action?command=open'),
          'Could not send open command',
          fn);
      }
    };
  })

  .factory('VentilationService', function ($http, HttpWrapper) {
    return {
      getStatus: function (fn) {
        HttpWrapper.handleResult($http.get('/storage_fan'), 'Could not fetch the ventilation status', fn);
      },
      sendLevel: function (level, fn) {
        HttpWrapper.handleResult(
          $http.post('/storage_fan/action?set_level=' + level),
          'Could not send level',
          fn);
      }
    };
  })

;