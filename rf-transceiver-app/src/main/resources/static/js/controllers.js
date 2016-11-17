'use strict';

/* Controllers */

angular.module('project.controllers', [])

    .controller('TestProxyRules', function ($scope, responseCodes, $timeout, $modal, RulesService, KeyValueStore) {
        $scope.KeyValueStore = KeyValueStore;
        $scope.responseCodes = responseCodes;

        $scope.isNotEmpty = function (object) {
            return Object.keys(object).length > 0;
        };

        $scope.shorten = function (text) {
            if (text !== undefined) {
                return text.substring(0, 30) + "...";
            }
            return text;
        };


        $scope.showRule = function (key) {
            var modalInstance = $modal.open({
                templateUrl: 'templates/rule-viewer.html',
                controller: function ($scope, $modalInstance, rule, key) {
                    $scope.rule = rule;
                    $scope.key = key;
                    $scope.selectedTab = 1;
                    $scope.existing = key !== undefined;

                    if (rule.mock_action) {
                        $scope.action_type = "mock";
                    } else if (rule.proxy_action) {
                        $scope.action_type = "proxy";
                    } else {
                        $scope.action_type = "proxy";
                        $scope.rule.proxy_action = {};
                    }

                    $scope.isTabSelected = function (tabId) {
                        return $scope.selectedTab == tabId;
                    };
                    $scope.onSelectTab = function (tabId) {
                        $scope.selectedTab = tabId;
                    };
                    $scope.actionTypeSelected = function () {
                        if ($scope.action_type === "mock") {
                            $scope.rule.mock_action = {};
                            delete $scope.rule.proxy_action;
                        } else {
                            $scope.rule.proxy_action = {};
                            delete $scope.rule.mock_action;
                        }
                    };

                    $scope.addXpath = function () {
                        if (!$scope.rule.hasOwnProperty("xpath_criteria")) {
                            $scope.rule.xpath_criteria = [];
                        }
                        var newObj = {};
                        newObj.xpath = $scope.newXpath;
                        newObj.regex = $scope.newXpathRegex;
                        $scope.rule.xpath_criteria.push(newObj);
                        $scope.newXpath = "";
                        $scope.newXpathRegex = "";
                    };
                    $scope.addJsonPath = function () {
                        if (!$scope.rule.hasOwnProperty("json_path_criteria")) {
                            $scope.rule.json_path_criteria = [];
                        }
                        var newObj = {};
                        newObj.json_path = $scope.newJsonPath;
                        newObj.regex = $scope.newJsonPathRegex;
                        $scope.rule.json_path_criteria.push(newObj);
                        $scope.newJsonPath = "";
                        $scope.newJsonPathRegex = "";
                    };
                    $scope.deleteFromArray = function (array, id) {
                        var newArray = [];
                        for (var i = 0; i < array.length; i++) {
                            if (id != i) {
                                newArray.push(array[i]);
                            }
                        }
                        return newArray;
                    };
                    $scope.addResponseHeader = function () {
                        if (!$scope.rule.mock_action.hasOwnProperty("response_headers")) {
                            $scope.rule.mock_action.response_headers = [];
                        }
                        var newObj = {};
                        newObj.header = $scope.newResponseHeader;
                        newObj.value = $scope.newResponseHeaderValue;
                        $scope.rule.mock_action.response_headers.push(newObj);
                        $scope.newResponseHeader = "";
                        $scope.newResponseHeaderValue = "";
                    };
                    $scope.deleteResponseHeader = function (id) {
                        $scope.rule.mock_action.response_headers = $scope.deleteFromArray($scope.rule.mock_action.response_headers, id);
                        if ($scope.rule.mock_action.response_headers.length == 0) {
                            delete $scope.rule.mock_action.response_headers;
                        }
                    };
                    $scope.deleteTargetHost = function (id) {
                        $scope.rule.proxy_action.target_hosts = $scope.deleteFromArray($scope.rule.proxy_action.target_hosts, id);
                        if ($scope.rule.proxy_action.target_hosts.length == 0) {
                            delete $scope.rule.proxy_action.target_hosts;
                        }
                    };
                    $scope.addTargetHost = function () {
                        if (!$scope.rule.proxy_action.hasOwnProperty("target_hosts")) {
                            $scope.rule.proxy_action.target_hosts = [];
                        }
                        $scope.rule.proxy_action.target_hosts.push($scope.newTargetHost);
                        $scope.newTargetHost = "";
                    };

                    $scope.deleteJsonPath = function (id) {
                        $scope.rule.json_path_criteria = $scope.deleteFromArray($scope.rule.json_path_criteria, id);
                        if ($scope.rule.json_path_criteria.length == 0) {
                            delete $scope.rule.json_path_criteria;
                        }
                    };
                    $scope.deleteXpath = function (id) {
                        $scope.rule.xpath_criteria = $scope.deleteFromArray($scope.rule.xpath_criteria, id);
                        if ($scope.rule.xpath_criteria.length == 0) {
                            delete $scope.rule.xpath_criteria;
                        }
                    };
                    $scope.delete = function () {
                        var result = {};
                        result.action = "delete";
                        result.key = $scope.key;
                        $modalInstance.close(result);
                    };
                    $scope.submit = function () {
                        var result = {};
                        result.action = "submit";
                        result.key = $scope.key;
                        result.rule = $scope.rule;
                        $modalInstance.close(result);
                    };
                    $scope.close = function () {
                        var result = {};
                        result.action = "close";
                        $modalInstance.close(result);
                    };
                    $scope.clearTimeout = function () {
                        delete $scope.rule.timeout_at;
                    };
                    $scope.addTimeout = function (inc) {
                        if ($scope.rule.timeout_at == undefined) {
                            $scope.rule.timeout_at = new Date().getTime();
                        }
                        $scope.rule.timeout_at += (inc * 1000 * 60);
                    }
                },
                resolve: {
                    rule: function () {
                        var r = $scope.rules[key];
                        if (r == undefined) {
                            r = {};
                            r.enabled = true;
                        }
                        return angular.copy(r);
                    },
                    key: function () {
                        return key;
                    }
                }
            });

            modalInstance.result.then(function (result) {
                // OK, modal closed
                if (result.action === 'delete') {
                    RulesService.deleteRule(result.key, function () {
                        delete $scope.rules[result.key];
                        $scope.updateSortedKeys();
                    });
                } else if (result.action === 'submit') {
                    RulesService.putRule(result.key, result.rule, function () {
                        $scope.rules[result.key] = result.rule;
                        $scope.updateSortedKeys();
                    });
                }
            }, function () {
                // dismissed, do nothing
            });
        };

        $scope.updateSortedKeys = function () {
            var sortedKeys = [];
            angular.forEach($scope.rules, function (value, key) {
                sortedKeys.push(key);
            });
            sortedKeys.sort();
            $scope.sortedKeys = sortedKeys;
        };

        // start to get the data in the background (async)
        RulesService.list(function (data) {
            $scope.rules = data;
            $scope.updateSortedKeys();
        });

    })

    .controller('TaskPicker', function ($scope, $location) {
        $scope.go = function (url) {
            $location.path(url);
        };
    })

    .controller('Garage', function ($scope, $location, GarageService) {
        $scope.go = function (url) {
            $location.path(url);
        };
        $scope.sendOpen = function () {
            GarageService.sendOpen(function () {
            });
        };
        $scope.sendClose = function () {
            GarageService.sendClose(function () {
            });
        };

        $scope.status = {};

        GarageService.getStatus(function (status) {
            $scope.status['door'] = status['door_state'];
            $scope.status['light'] = status['light_state'];
        });
    })

    .controller('Heater', function ($scope, $location, HeaterService) {
        $scope.go = function (url) {
            $location.path(url);
        };

        $scope.tempToString = function (temp) {
            var str = temp.toString();
            while (str.length < 4) {
                str = "0" + str;
            }
            return str.substring(0, str.length - 2) + "." + str.substring(str.length - 2);
        };
        $scope.stringToTemp = function (str) {
            var temp = parseFloat(str);
            return Math.round(temp * 100);
        };

        $scope.refresh = function () {
            HeaterService.getStatus(function (status) {
                $scope.automatic_mode = status['automatic_mode'];
                $scope.target_temperature = $scope.tempToString(status['target_temperature']);
                $scope.target_heater_status = status['target_heater_status'];
                $scope.target_temperature_input = $scope.target_temperature;
            });
        };
        $scope.refresh();

        $scope.changeTargetTemperatur = function (value) {
            HeaterService.postAction('set_target_temperature', $scope.stringToTemp(value), function () {
                $scope.refresh();
            });
        };

        $scope.switchToAutomatic = function () {
            HeaterService.postAction('switch_automatic', true, function () {
                $scope.refresh();
            });
        };

        $scope.switchToManual = function () {
            HeaterService.postAction('switch_automatic', false, function () {
                $scope.refresh();
            });
        };

        $scope.switchOff = function () {
            HeaterService.postAction('switch_heater', false, function () {
                $scope.refresh();
            });
        };

        $scope.switchOn = function () {
            HeaterService.postAction('switch_heater', true, function () {
                $scope.refresh();
            });
        };

    })

;