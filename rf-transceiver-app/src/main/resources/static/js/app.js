'use strict';

angular.module('project', [
  'ngRoute',
  'project.services',
  'project.controllers',
  'ui.bootstrap'
])
  .config(function ($routeProvider) {
    $('#errorMsg').hide();
    $('#workingMsg').hide();

    $routeProvider
      .when('/', {
        controller: 'TaskPicker',
        templateUrl: 'templates/taskpicker.html'
      })
      .when('/garage', {
        controller: 'Garage',
        templateUrl: 'templates/garage.html'
      })
      .when('/heater', {
        controller: 'Heater',
        templateUrl: 'templates/heater.html'
      })
      .otherwise({
        redirectTo: '/'
      });
  })
;