function f() {
    return this === null;
};

function g() {
    if (!f.apply(9)) {}
}

oomTest(g);
