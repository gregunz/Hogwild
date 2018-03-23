package computations

import org.scalatest.FunSuite
import util.Label

class SVMTest extends FunSuite {

  test("Label values are correct") {
    assert(Label.CCAT.id === 1)
    assert(Label.Else.id === -1)
  }
}
