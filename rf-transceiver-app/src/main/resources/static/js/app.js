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
      .when('/ventilation', {
        controller: 'Ventilation',
        templateUrl: 'templates/ventilation.html'
      })
      .otherwise({
        redirectTo: '/'
      });
  })
;