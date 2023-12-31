module com.udacity.catpoint.security {

    requires com.google.gson;
    requires com.udacity.catpoint.image;
    requires java.sql;
    requires miglayout;
    requires java.desktop;
    requires com.google.common;
    requires java.prefs;

    opens com.udacity.catpoint.security.data to com.google.gson;

    exports com.udacity.catpoint.security.data;
}