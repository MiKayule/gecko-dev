// |reftest| skip-if(!this.hasOwnProperty('Temporal')) -- Temporal is not enabled unconditionally
// Copyright (C) 2020 Igalia, S.L. All rights reserved.
// This code is governed by the BSD license found in the LICENSE file.

/*---
esid: sec-temporal.timezone.prototype.getoffsetstringfor
description: timeZone.getOffsetNanosecondsFor not called when argument cannot be converted to Temporal.Instant
features: [Temporal]
---*/

const timeZone = Temporal.TimeZone.from("UTC");
let called = false;
timeZone.getOffsetNanosecondsFor = () => called = true;
assert.throws(TypeError, () => timeZone.getOffsetStringFor(undefined), "undefined");
assert.throws(TypeError, () => timeZone.getOffsetStringFor(null), "null");
assert.throws(TypeError, () => timeZone.getOffsetStringFor(true), "boolean");
assert.throws(RangeError, () => timeZone.getOffsetStringFor(""), "empty string");
assert.throws(TypeError, () => timeZone.getOffsetStringFor(Symbol()), "Symbol");
assert.throws(TypeError, () => timeZone.getOffsetStringFor(5), "number");
assert.throws(TypeError, () => timeZone.getOffsetStringFor(5n), "bigint");
assert.throws(RangeError, () => timeZone.getOffsetStringFor({}), "plain object");
assert.sameValue(called, false);

reportCompare(0, 0);
