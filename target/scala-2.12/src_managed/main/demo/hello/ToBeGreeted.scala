// Generated by the Scala Plugin for the Protocol Buffer Compiler.
// Do not edit!
//
// Protofile syntax: PROTO3

package demo.hello

@SerialVersionUID(0L)
final case class ToBeGreeted(
    person: scala.Option[demo.hello.ToBeGreeted.Person] = None,
    msg: _root_.scala.Predef.String = ""
    ) extends scalapb.GeneratedMessage with scalapb.Message[ToBeGreeted] with scalapb.lenses.Updatable[ToBeGreeted] {
    @transient
    private[this] var __serializedSizeCachedValue: _root_.scala.Int = 0
    private[this] def __computeSerializedValue(): _root_.scala.Int = {
      var __size = 0
      if (person.isDefined) { __size += 1 + _root_.com.google.protobuf.CodedOutputStream.computeUInt32SizeNoTag(person.get.serializedSize) + person.get.serializedSize }
      if (msg != "") { __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(2, msg) }
      __size
    }
    final override def serializedSize: _root_.scala.Int = {
      var read = __serializedSizeCachedValue
      if (read == 0) {
        read = __computeSerializedValue()
        __serializedSizeCachedValue = read
      }
      read
    }
    def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): Unit = {
      person.foreach { __v =>
        _output__.writeTag(1, 2)
        _output__.writeUInt32NoTag(__v.serializedSize)
        __v.writeTo(_output__)
      };
      {
        val __v = msg
        if (__v != "") {
          _output__.writeString(2, __v)
        }
      };
    }
    def mergeFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): demo.hello.ToBeGreeted = {
      var __person = this.person
      var __msg = this.msg
      var _done__ = false
      while (!_done__) {
        val _tag__ = _input__.readTag()
        _tag__ match {
          case 0 => _done__ = true
          case 10 =>
            __person = Option(_root_.scalapb.LiteParser.readMessage(_input__, __person.getOrElse(demo.hello.ToBeGreeted.Person.defaultInstance)))
          case 18 =>
            __msg = _input__.readString()
          case tag => _input__.skipField(tag)
        }
      }
      demo.hello.ToBeGreeted(
          person = __person,
          msg = __msg
      )
    }
    def getPerson: demo.hello.ToBeGreeted.Person = person.getOrElse(demo.hello.ToBeGreeted.Person.defaultInstance)
    def clearPerson: ToBeGreeted = copy(person = None)
    def withPerson(__v: demo.hello.ToBeGreeted.Person): ToBeGreeted = copy(person = Option(__v))
    def withMsg(__v: _root_.scala.Predef.String): ToBeGreeted = copy(msg = __v)
    def getFieldByNumber(__fieldNumber: _root_.scala.Int): scala.Any = {
      (__fieldNumber: @_root_.scala.unchecked) match {
        case 1 => person.orNull
        case 2 => {
          val __t = msg
          if (__t != "") __t else null
        }
      }
    }
    def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
      require(__field.containingMessage eq companion.scalaDescriptor)
      (__field.number: @_root_.scala.unchecked) match {
        case 1 => person.map(_.toPMessage).getOrElse(_root_.scalapb.descriptors.PEmpty)
        case 2 => _root_.scalapb.descriptors.PString(msg)
      }
    }
    def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
    def companion = demo.hello.ToBeGreeted
}

object ToBeGreeted extends scalapb.GeneratedMessageCompanion[demo.hello.ToBeGreeted] {
  implicit def messageCompanion: scalapb.GeneratedMessageCompanion[demo.hello.ToBeGreeted] = this
  def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): demo.hello.ToBeGreeted = {
    require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
    val __fields = javaDescriptor.getFields
    demo.hello.ToBeGreeted(
      __fieldsMap.get(__fields.get(0)).asInstanceOf[scala.Option[demo.hello.ToBeGreeted.Person]],
      __fieldsMap.getOrElse(__fields.get(1), "").asInstanceOf[_root_.scala.Predef.String]
    )
  }
  implicit def messageReads: _root_.scalapb.descriptors.Reads[demo.hello.ToBeGreeted] = _root_.scalapb.descriptors.Reads{
    case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
      require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
      demo.hello.ToBeGreeted(
        __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).flatMap(_.as[scala.Option[demo.hello.ToBeGreeted.Person]]),
        __fieldsMap.get(scalaDescriptor.findFieldByNumber(2).get).map(_.as[_root_.scala.Predef.String]).getOrElse("")
      )
    case _ => throw new RuntimeException("Expected PMessage")
  }
  def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = HelloProto.javaDescriptor.getMessageTypes.get(0)
  def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = HelloProto.scalaDescriptor.messages(0)
  def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = {
    var __out: _root_.scalapb.GeneratedMessageCompanion[_] = null
    (__number: @_root_.scala.unchecked) match {
      case 1 => __out = demo.hello.ToBeGreeted.Person
    }
    __out
  }
  lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_]] = Seq[_root_.scalapb.GeneratedMessageCompanion[_]](
    _root_.demo.hello.ToBeGreeted.Person
  )
  def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
  lazy val defaultInstance = demo.hello.ToBeGreeted(
  )
  @SerialVersionUID(0L)
  final case class Person(
      name: _root_.scala.Predef.String = ""
      ) extends scalapb.GeneratedMessage with scalapb.Message[Person] with scalapb.lenses.Updatable[Person] {
      @transient
      private[this] var __serializedSizeCachedValue: _root_.scala.Int = 0
      private[this] def __computeSerializedValue(): _root_.scala.Int = {
        var __size = 0
        if (name != "") { __size += _root_.com.google.protobuf.CodedOutputStream.computeStringSize(1, name) }
        __size
      }
      final override def serializedSize: _root_.scala.Int = {
        var read = __serializedSizeCachedValue
        if (read == 0) {
          read = __computeSerializedValue()
          __serializedSizeCachedValue = read
        }
        read
      }
      def writeTo(`_output__`: _root_.com.google.protobuf.CodedOutputStream): Unit = {
        {
          val __v = name
          if (__v != "") {
            _output__.writeString(1, __v)
          }
        };
      }
      def mergeFrom(`_input__`: _root_.com.google.protobuf.CodedInputStream): demo.hello.ToBeGreeted.Person = {
        var __name = this.name
        var _done__ = false
        while (!_done__) {
          val _tag__ = _input__.readTag()
          _tag__ match {
            case 0 => _done__ = true
            case 10 =>
              __name = _input__.readString()
            case tag => _input__.skipField(tag)
          }
        }
        demo.hello.ToBeGreeted.Person(
            name = __name
        )
      }
      def withName(__v: _root_.scala.Predef.String): Person = copy(name = __v)
      def getFieldByNumber(__fieldNumber: _root_.scala.Int): scala.Any = {
        (__fieldNumber: @_root_.scala.unchecked) match {
          case 1 => {
            val __t = name
            if (__t != "") __t else null
          }
        }
      }
      def getField(__field: _root_.scalapb.descriptors.FieldDescriptor): _root_.scalapb.descriptors.PValue = {
        require(__field.containingMessage eq companion.scalaDescriptor)
        (__field.number: @_root_.scala.unchecked) match {
          case 1 => _root_.scalapb.descriptors.PString(name)
        }
      }
      def toProtoString: _root_.scala.Predef.String = _root_.scalapb.TextFormat.printToUnicodeString(this)
      def companion = demo.hello.ToBeGreeted.Person
  }
  
  object Person extends scalapb.GeneratedMessageCompanion[demo.hello.ToBeGreeted.Person] {
    implicit def messageCompanion: scalapb.GeneratedMessageCompanion[demo.hello.ToBeGreeted.Person] = this
    def fromFieldsMap(__fieldsMap: scala.collection.immutable.Map[_root_.com.google.protobuf.Descriptors.FieldDescriptor, scala.Any]): demo.hello.ToBeGreeted.Person = {
      require(__fieldsMap.keys.forall(_.getContainingType() == javaDescriptor), "FieldDescriptor does not match message type.")
      val __fields = javaDescriptor.getFields
      demo.hello.ToBeGreeted.Person(
        __fieldsMap.getOrElse(__fields.get(0), "").asInstanceOf[_root_.scala.Predef.String]
      )
    }
    implicit def messageReads: _root_.scalapb.descriptors.Reads[demo.hello.ToBeGreeted.Person] = _root_.scalapb.descriptors.Reads{
      case _root_.scalapb.descriptors.PMessage(__fieldsMap) =>
        require(__fieldsMap.keys.forall(_.containingMessage == scalaDescriptor), "FieldDescriptor does not match message type.")
        demo.hello.ToBeGreeted.Person(
          __fieldsMap.get(scalaDescriptor.findFieldByNumber(1).get).map(_.as[_root_.scala.Predef.String]).getOrElse("")
        )
      case _ => throw new RuntimeException("Expected PMessage")
    }
    def javaDescriptor: _root_.com.google.protobuf.Descriptors.Descriptor = demo.hello.ToBeGreeted.javaDescriptor.getNestedTypes.get(0)
    def scalaDescriptor: _root_.scalapb.descriptors.Descriptor = demo.hello.ToBeGreeted.scalaDescriptor.nestedMessages(0)
    def messageCompanionForFieldNumber(__number: _root_.scala.Int): _root_.scalapb.GeneratedMessageCompanion[_] = throw new MatchError(__number)
    lazy val nestedMessagesCompanions: Seq[_root_.scalapb.GeneratedMessageCompanion[_]] = Seq.empty
    def enumCompanionForFieldNumber(__fieldNumber: _root_.scala.Int): _root_.scalapb.GeneratedEnumCompanion[_] = throw new MatchError(__fieldNumber)
    lazy val defaultInstance = demo.hello.ToBeGreeted.Person(
    )
    implicit class PersonLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, demo.hello.ToBeGreeted.Person]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, demo.hello.ToBeGreeted.Person](_l) {
      def name: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.name)((c_, f_) => c_.copy(name = f_))
    }
    final val NAME_FIELD_NUMBER = 1
  }
  
  implicit class ToBeGreetedLens[UpperPB](_l: _root_.scalapb.lenses.Lens[UpperPB, demo.hello.ToBeGreeted]) extends _root_.scalapb.lenses.ObjectLens[UpperPB, demo.hello.ToBeGreeted](_l) {
    def person: _root_.scalapb.lenses.Lens[UpperPB, demo.hello.ToBeGreeted.Person] = field(_.getPerson)((c_, f_) => c_.copy(person = Option(f_)))
    def optionalPerson: _root_.scalapb.lenses.Lens[UpperPB, scala.Option[demo.hello.ToBeGreeted.Person]] = field(_.person)((c_, f_) => c_.copy(person = f_))
    def msg: _root_.scalapb.lenses.Lens[UpperPB, _root_.scala.Predef.String] = field(_.msg)((c_, f_) => c_.copy(msg = f_))
  }
  final val PERSON_FIELD_NUMBER = 1
  final val MSG_FIELD_NUMBER = 2
}