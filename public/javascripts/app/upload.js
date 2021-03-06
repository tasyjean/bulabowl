define(["backbone", "swfupload", "underscore"], function (Backbone, SWFUpload, _) {

    var html5 = false;
    try {
        var xhr = new XMLHttpRequest();
        html5 = !!(xhr && ('upload' in xhr) && ('onprogress' in xhr.upload));
    } catch (e) {

    }
    var UploadView = Backbone.View.extend({


        _file:null,
        _currentFile:null,
        events:{
            'click .cancel':"cancelUpload",
            'click .remove':"removeFile"
        },
        initialize:function (options) {


            options = options || {};
            var button = this.$el.find(options.replace ? ".remove" : ".upload-button").click(function () {
                return false;
            });
            if (app_config.replaceTrack) {
                button.html("replace audio");
            }

            button.html("<span class='trigger'>" + button.html() + "</span>");

            var hit = $("<span class='uploader'></span>").appendTo(button);
            if (button.width() == 0) {
                var self = this;
                // allow for css redraw
                setTimeout(function () {
                    self.finalize(options, button, hit);
                }, 2);
            } else {
                this.finalize(options, button, hit);
            }


        },
        _createButton:function () {

            return button;
        },
        canUpload:function () {
            return true;
        },
        finalize:function (options, button, hit) {


            this.swf = new SWFUpload({
                upload_url:app_config.uri.upload + options.uri,

                flash_url:"/assets/swfupload.swf",
                file_size_limit:options.limit || "4 MB",
                file_types:options.types || "*",
                button_window_mode:SWFUpload.WINDOW_MODE.TRANSPARENT,
                button_cursor:SWFUpload.CURSOR,

                upload_start_handler:_.bind(this._onUploadStarted, this),
                upload_progress_handler:_.bind(this._onUploadProgress, this),
                upload_success_handler:_.bind(this._onUploadSuccess, this),
                upload_complete_handler:_.bind(this._onUploadComplete, this),
                upload_error_handler:_.bind(this._onUploadError, this),
                file_dialog_complete_handler:_.bind(this._onDialogComplete, this),


                button_width:button.width(),
                button_height:button.height(),
                button_text:"",
                debug:true,
                debug_handler:function () {
                    self.debug(arguments);
                },
                button_placeholder:hit[0]



            });
            this.attachElements();


            this.bindTo(this)
        },
        attachElements:function () {
            this.$progress = this.options.progressSelector ? $(this.options.progressSelector) : this.$(".upload-progress");
            this.$percent = this.$progress.find(".percent");
            this.$duration = this.$progress.find(".duration");
        },

        remove:function () {

            this.swf.destroy();
            return this;
        },
        bindTo:function (view) {
            this.setElement(view.el, true);

            this.$wrapper = this.$(".progress-wrapper");
            this.$bar = this.$(".bar");


            this.$status = this.$(".status");
            this.$cancel = this.$(".cancel");
            this.$remove = this.$(".remove");
            this.$file = this.$(".file");
            this.$hit = this.$(".hit");

        },
        _onDialogComplete:function (numFilesSelected) {

            if (numFilesSelected == 1) {
                this.swf.setPostParams({
                    token:app_config.token,
                    session:app_config.session()

                });
                this.trigger("beforeStarted", []);
                this._file = null;
                this.render();
                this.$hit.hide();

                this.$progress.show();
                this.swf.startUpload();

            }

        },
        setPostParam:function (key, value) {
            // TODO Implement post params update
            return this;
        },

        cancelUpload:function () {
            if (this._file) {
                this.swf.stopUpload();
                this.swf.cancelUpload();
            }
            this.$progress.delay(200).fadeOut("slow");

        },
        removeFile:function () {


        },
        _restoreFromError:function () {
            /*if (!this._currentFile) {
             this.$hit.show("slow");
             //this.$progress.hide("");
             this.$wrapper.hide("slow");
             } */
            this.$progress.fadeOut();
            /*if (this._currentFile) {
             this._onStatusChange("finished", this._currentFile);
             } else {

             } */
            this._onStatusChange("error")
            //}
        },
        _onUploadError:function (file, errorCode) {
            switch (errorCode) {
                case SWFUpload.UPLOAD_ERROR.UPLOAD_STOPPED:
                    this.trigger("stopped");

                    break;
                case SWFUpload.UPLOAD_ERROR.FILE_CANCELLED:
                    this._restoreFromError();

                    if (errorCode == SWFUpload.UPLOAD_ERROR.FILE_CANCELLED)
                        this.trigger("canceled");
                    break;

                default:
                    this.trigger("error");
                    this._restoreFromError();
                    break;

            }
            this._file = null;
        },
        reset:function () {
            //this._file = null;
            this._render();
            this.$wrapper.hide();
            this.$progress.hide();
        },
        _onUploadStarted:function (file) {

            this._onStatusChange("uploading")


            this._file = file;

            this.$file.html(file.name);


            this.$progress.delay(500).fadeIn("slow");
            this.$percent.html("0%");
            this.$duration.html("--:--");

            this.render()
            this.trigger("started");
        },
        _onUploadProgress:function (file, bytes, total) {

            this._file = file;
            this.render()
        },
        _onUploadSuccess:function (file, serverData, receivedResponse) {
            this._file = file;
            this.$progress.fadeOut();
            this._onStatusChange('uploaded', file);

            //this.$remove.show();


            if (serverData) {
                var info = $.parseJSON(serverData);


                this.trigger("uploaded", info);


            }


        },
        fileName:function () {
            return (this._currentFile || {}).name;
        },
        _onStatusChange:function (status, file) {
            if (file) {
                this._currentFile = file;
                this.$file.html(this._currentFile.name);
            }
            this.$wrapper.removeClass().addClass("progress-wrapper active " + status);
            this.$wrapper.find(".status").text(status);
            return this;
        },
        _onUploadDone:function (file) {


        },

        _onUploadComplete:function (file) {
            this._file = file;

        },
        render:function () {

            this._render(this._file);

        },
        _render:function (file) {
            var percent = file ? file.percentUploaded.toFixed(2) : 0;
            var remaining = file ? SWFUpload.speed.formatTime(file.timeRemaining) : "--:--";


            this.$bar.css({width:percent + "%"})
            this.$percent.html(percent);

            this.$duration.html(remaining);
        }

    })

    if (html5) {
        UploadView = UploadView.extend({
            cancelUpload:function () {
                this.$input.trigger("html5_upload.cancelOne");
                this._onUploadError(this._file, SWFUpload.UPLOAD_ERROR.UPLOAD_STOPPED)
                this._onUploadError(this._file, SWFUpload.UPLOAD_ERROR.FILE_CANCELLED);
            },
            _extraFields:{},
            setPostParam:function (key, value) {
                var options = this.$input[0].html5_upload.options;
                if (value) {
                    this._extraFields[key] = value
                } else {
                    delete this._extraFields[key];
                }
            },
            finalize:function (options, button, hit) {
                var self = this;
                SWFUpload.prototype.initSWFUpload = function () {
                }
                SWFUpload.prototype.startUpload = function () {
                };
                SWFUpload.prototype.setPostParams = function (params) {
                    self._extraFields = $.extend({}, params, self._extraFields)
                }
                var swf = this.swf = new SWFUpload();
                var input = this.$input = $("<input type='file'/>").appendTo(this.el).css({position:"absolute", "left":"-999999px"})
                swf.fileSpeedStats = {};
                swf.speedSettings = {};
                swf.settings = {moving_average_history_size:10}
                button.click(function () {
                    if (self.canUpload()) {
                        input.click();
                    }
                    return false;
                })
                options.types = _((options.types || "").split(";")).map(function (value, index) {
                    return value.replace("*.", "");
                })
                var _file = {};

                function extend() {
                    _file = SWFUpload.speed.extendFile(_file, swf.fileSpeedStats);
                }


                input.html5_upload({
                    fieldName:"Filedata",
                    url:app_config.uri.upload + options.uri,


                    sendBoundary:window.FormData || $.browser.mozilla,
                    beforeFormData:function (params) {
                        return $.extend(params, {
                            token:app_config.token,
                            session:app_config.session()

                        }, self._extraFields);
                    },
                    onStartOne:function (event, file, name, number, total) {
                        _file = self._file = {
                            name:name,
                            id:(new Date().getTime()),
                            size:file.size || file.fileSize
                        }
                        if (options.types.length) {
                            if (!options.types.indexOf(name.split(".").pop()) == -1) {
                                self._onUploadError(_file, SWFUpload.QUEUE_ERROR.INVALID_FILETYPE);
                                return false;
                            }

                        }

                        self._onDialogComplete(total);

                        setTimeout(function () {
                            extend();

                            self._onUploadStarted(_file);
                        }, 2);
                        return true;

                    },

                    onProgress:function (event, progress, name, number, total, res) {

                        swf.updateTracking(_file, res.loaded);
                        extend();
                        self._onUploadProgress(_file);


                    },
                    setName:function (text) {
                        // $("#progress_report_name").text(text);
                    },
                    setStatus:function (text) {
                        //$("#progress_report_status").text(text);
                    },
                    setProgress:function (val) {
                        //$("#progress_report_bar").css('width', Math.ceil(val * 100) + "%");
                    },
                    onFinishOne:function (event, response, name, number, total) {
                        self._onUploadSuccess(_file, response);
                    },
                    onError:function (event, name, error) {
                        //alert('error while uploading file ' + name);
                        self._onUploadError(null, SWFUpload.UPLOAD_ERROR.IO_ERROR);
                    }
                });
                this.attachElements();


                this.bindTo(this)
            }
        })
    }


    var ReplaceUploadView = UploadView.extend({
        events:{

        },
        initialize:function (options, uploadView, mainView) {
            this.mainView = mainView;
            this.uploadView = uploadView;
            options = $.extend({}, options, {replace:true});
            this.proxy();
            this._super("initialize", [options]);


        },
        proxy:function () {
            var methods = ['reset', 'render', '_onUploadProgress',
                '_onUploadSuccess', '_onUploadStarted', '_onUploadError', '_onDialogComplete'];
            var self = this;
            var uploadView = this.uploadView;
            var mainView = this.mainView;
            var uploadViewDefaultSWF = uploadView.swf;
            var currentFile = !_.isEmpty(this.model.get("file")) ? {
                name:this.model.get("fileName")
            } : null;


            function proxy(method, name) {
                return function () {
                    mainView._active = true;
                    uploadView.swf = self.swf;
                    // replace first file if avail
                    if (name == "_onDialogComplete" && !uploadView._currentFile) {
                        uploadView._currentFile = currentFile;
                    }
                    var results = method.apply(uploadView, arguments);
                    uploadView.swf = uploadViewDefaultSWF;
                    mainView._active = false;
                    return results;
                }
            }

            // remove events
            uploadView.undelegateEvents();
            _(methods).each(function (value, key) {


                self[value] = proxy(self[value], value)
            });
            uploadView.cancelUpload = proxy(uploadView.cancelUpload);
            // reapply events
            uploadView.delegateEvents();
        }
    })

    return{
        View:UploadView,
        ReplaceView:ReplaceUploadView

    }
})
;

