// OOM during reporting warning should be handled.

oomTest(function(){
  new Function("return; 0;");
});
