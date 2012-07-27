define(["dropdown"], function () {
    var initialize = function () {
        // Pass in our Router module and call it's initialize function

        controllers.init();

    }

    var controllers = {

        pages:{
            "edit_track":["app/controller/track"],
            "new_album":["app/controller/album"],
            "edit_album":["app/controller/album"],
            "pick_tags":["app/controller/pickTags"],
            "album":["app/controller/display"],
            "track":["app/controller/display"]
        },
        init:function () {

            $('.dropdown-toggle').dropdown();
            app_config.session = $("input[name='session']").val()
            var path = location.pathname.split("/")[1];
            if (path in this.pages) {
                require(this.pages[path], function (ctr) {
                    window.view = new ctr.View();
                });
            }
        }

    }

    return {
        initialize:initialize
    };
});