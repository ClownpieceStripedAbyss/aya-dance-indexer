module moe.kiva.pypy.mirror {
  requires static org.jetbrains.annotations;
  requires com.google.gson;
  requires java.net.http;
  requires kala.base;
  requires kala.collection;
  requires pinyin;

  exports moe.kiva;
  opens moe.kiva to com.google.gson;
}
