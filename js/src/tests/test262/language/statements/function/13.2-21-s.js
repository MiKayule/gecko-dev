// Copyright (c) 2012 Ecma International.  All rights reserved.
// This code is governed by the BSD license found in the LICENSE file.

/*---
es5id: 13.2-21-s
description: >
    StrictMode - reading a property named 'caller' of function objects
    is not allowed outside the function
flags: [noStrict]
---*/

function foo () {"use strict";}

assert.throws(TypeError, function() {
    var temp = foo.caller;
});

reportCompare(0, 0);
