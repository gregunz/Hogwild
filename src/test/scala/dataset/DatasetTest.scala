package dataset

import org.scalatest.FunSuite

class DatasetTest extends FunSuite {

  test("number of IDs is correct") {
    val officialTotal = 781265
    assert(Dataset.didSet.size === officialTotal)
  }

  test("ID from labels are the same as vectors") {
    assert((Dataset.didSet -- Dataset.labels.keySet).isEmpty)
  }
}
