var bytelength = (function() {
var def = function({ $CHUNK }) { this.printer = new Print();
  this.$CHUNK = Calang['BytesValue'].newInstance();
    this.$CHUNK.setValue($CHUNK);
  this.$LENGTH = Calang['IntegerValue'].newInstance();
};
def.prototype = {
  __START: async function() {
this.$LENGTH.setValue(this.$CHUNK.sendMessage("|.|", []));
  },
  run: async function() { await this.__START(); this.printer.flush(); return { $LENGTH:this.$LENGTH }; }
};
return def; })();
var password_input = (function() {
var def = function({  }) { this.printer = new Print();
  this.$CLICK_PROGRAM = Calang['ProgramValue'].newInstance();
  this.$MODAL_RESULT = Calang['BooleanValue'].newInstance();
  this.$MODAL_ELEMENT = Calang['ModalElementValue'].newInstance();
  this.$TEXT_RECORD = Calang['BytesValue'].newInstance();
};
def.prototype = {
  __MODAL_CLOSE: async function() {
this.$MODAL_ELEMENT.setValue(this.$MODAL_ELEMENT.sendMessage("close!", []));
  },
  __MODAL_OPEN: async function() {
this.$MODAL_ELEMENT.setValue(this.$MODAL_ELEMENT.sendMessage("display!", []));
  },
  __START: async function() {
await this.__MODAL_OPEN();
this.$CLICK_PROGRAM.setValue(this.$MODAL_ELEMENT.sendMessage("...", []));
await this.$CLICK_PROGRAM.getValue().bindWith({  }).run()
.then(__ => {
  this.$MODAL_RESULT.setValue(__.$RES);
})
;
if(this.$MODAL_RESULT.getValue()) await this.__USER_CONFIRMS(); else await this.__USER_CANCELS();
await this.__MODAL_CLOSE();
  },
  __USER_CANCELS: async function() {
this.printer.append(`Good bye user, you'll be missed`);
  },
  __USER_CONFIRMS: async function() {
this.$TEXT_RECORD.setValue(this.$MODAL_ELEMENT.sendMessage("?", []));
this.printer.append(`Warm greeting, password is ${this.$TEXT_RECORD.getValue()}`);
  },
  run: async function() { await this.__START(); this.printer.flush(); return { $TEXT_RECORD:this.$TEXT_RECORD }; }
};
return def; })();
var prog = (function() {
var def = function({  }) { this.printer = new Print();
  this.$MESSAGE = Calang['BytesValue'].newInstance();
  this.$LENGTH = Calang['IntegerValue'].newInstance();
};
def.prototype = {
  __BEGIN: async function() {
await new password_input({  }).run()
.then(__ => {
  this.$MESSAGE.setValue(__.$TEXT_RECORD);
})
;
await new bytelength({ $CHUNK:this.$MESSAGE }).run()
.then(__ => {
  this.$LENGTH.setValue(__.$LENGTH);
})
;
await new tower({ $HEIGHT:this.$LENGTH }).run()
.then(__ => {
})
;
  },
  run: async function() { await this.__BEGIN(); this.printer.flush(); return {  }; }
};
return def; })();
var tower = (function() {
var def = function({ $HEIGHT }) { this.printer = new Print();
  this.$HEIGHT = Calang['IntegerValue'].newInstance();
    this.$HEIGHT.setValue($HEIGHT);
  this.$CURSOR = Calang['IntegerValue'].newInstance();
  this.$LOCAL_HEIGHT = Calang['IntegerValue'].newInstance();
  this.$FLAG = Calang['BooleanValue'].newInstance();
};
def.prototype = {
  __MAIN: async function() {
this.$LOCAL_HEIGHT.setValue("1");
this.$FLAG.setValue(this.$HEIGHT);
while(this.$FLAG.getValue()) await this.__PRINT_LINE();
  },
  __PRINT_COLUMN: async function() {
this.printer.append(`#`);
this.$FLAG.setValue(this.$LOCAL_HEIGHT.sendMessage("-", [this.$CURSOR]));
this.$CURSOR.setValue(this.$CURSOR.sendMessage("succ", []));
  },
  __PRINT_LINE: async function() {
this.$CURSOR.setValue("1");
this.$FLAG.setValue("1");
while(this.$FLAG.getValue()) await this.__PRINT_COLUMN();
this.printer.append(`\n`);
this.$FLAG.setValue(this.$HEIGHT.sendMessage("-", [this.$LOCAL_HEIGHT]));
this.$LOCAL_HEIGHT.setValue(this.$LOCAL_HEIGHT.sendMessage("succ", []));
  },
  run: async function() { await this.__MAIN(); this.printer.flush(); return {  }; }
};
return def; })();