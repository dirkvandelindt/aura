/*
 * Copyright (C) 2013 salesforce.com, inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/*jslint sub: true*/

//#include aura.locker.SecureAuraEvent

var SecureComponent = (function() {
  "use strict";

  function SecureComponent(component, key) {
    // Storing a reusable reference to the corresponding secure component into
    // the original component via the locker secret mechanism.
    var sc = getLockerSecret(component, "secure");
    if (sc) {
      return sc;
    }
    setLockerSecret(component, "secure", this); // backpointer
    // regular initialization:
    setLockerSecret(this, "key", key);
    setLockerSecret(this, "ref", component);
    // The shape of the component depends on the methods exposed in the definitions:
    var defs = component.getDef().methodDefs;
    if (defs) {
      defs.forEach(function (method) {
        Object.defineProperty(this, method.name, SecureThing.createFilteredMethod(method.name));
      }, this);
    }
  }

  SecureComponent.prototype = Object.create(null, {
    toString: {
      value : function() {
        return "SecureComponent: " + getLockerSecret(this, "ref") + "{ key: " + JSON.stringify(getLockerSecret(this, "key")) + " }";
      }
    },

    // these four super* methods are exposed as a temporary solution until we figure how to re-arrange the render flow
    "superRender": SecureThing.createFilteredMethod("superRender"),
    "superAfterRender": SecureThing.createPassThroughMethod("superAfterRender"),
    "superRerender": SecureThing.createFilteredMethod("superRerender"),
    "superUnrender": SecureThing.createFilteredMethod("superUnrender"),

    "isValid": SecureThing.createPassThroughMethod("isValid"),
    "isInstanceOf": SecureThing.createPassThroughMethod("isInstanceOf"),
    "addHandler": SecureThing.createPassThroughMethod("addHandler"),
    "destroy": SecureThing.createPassThroughMethod("destroy"),
    "isRendered": SecureThing.createPassThroughMethod("isRendered"),
    "getGlobalId": SecureThing.createPassThroughMethod("getGlobalId"),
    "getLocalId": SecureThing.createPassThroughMethod("getLocalId"),
    "getSuper": SecureThing.createFilteredMethod('getSuper'),
    "getReference": SecureThing.createFilteredMethod("getReference"),
    "clearReference": SecureThing.createPassThroughMethod("clearReference"),
    "autoDestroy": SecureThing.createPassThroughMethod("autoDestroy"),
    "isConcrete": SecureThing.createPassThroughMethod("isConcrete"),
    "addValueProvider": SecureThing.createPassThroughMethod("addValueProvider"),
    "getConcreteComponent": SecureThing.createFilteredMethod('getConcreteComponent'),
    "find": SecureThing.createFilteredMethod('find'),
    "set": SecureThing.createFilteredMethod("set"),
    "get": SecureThing.createFilteredMethod("get"),
    "getElement": SecureThing.createFilteredMethod("getElement"),
    "getElements": SecureThing.createFilteredMethod("getElements"),
    "getEvent": {
      value: function(name) {
        // system call to collect the low level aura event
        var event = getLockerSecret(this, "ref").getEvent(name);
        if (!event) {
          return event;
        }
        // shadowing the low level aura event with the component's key
        return new SecureAuraEvent(event, getLockerSecret(this, "key"));
      }
    }

  });

  SecureComponent.prototype.constructor = SecureComponent;

  return SecureComponent;
})();