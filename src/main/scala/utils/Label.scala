package utils

object Label extends Enumeration {
  type Label = Value

  val CCAT: Label.Value = Value(1)
  val Else: Label.Value = Value(-1)

  def fromInt(value: Int): Label = apply(value == 1)

  def apply(isCCAT: Boolean): Label = if (isCCAT) CCAT else Else
}