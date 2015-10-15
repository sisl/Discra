package spark.worker.grid

import breeze.numerics.pow

/** Object for grid used in multilinear interpolation. */
class Grid(
    private val cutPoints: Array[Array[Double]],
    private val cutCounts: Array[Int],
    private val cuts: Array[Double],
    private val index: Array[Int],
    private val weight: Array[Double],
    private val index2: Array[Int],
    private val weight2: Array[Double])
  extends Serializable {

  def length = cutCounts.product

  def dimensions = cutCounts.length

  /** Converts grid index to actual state. */
  def ind2x(ind: Int): Array[Double] = {
    var currInd = ind
    val point = new Array[Double](dimensions)

    var stride = cutCounts(0)
    for (i <- 1 until dimensions - 1) {
      stride *= cutCounts(i)
    }

    for (i <- dimensions - 2 to 0 by -1) {
      val rest = currInd % stride
      point(i + 1) = cutPoints(i + 1)((currInd - rest) / stride)
      currInd = rest
      stride /= cutCounts(i)
    }
    point(0) = cutPoints(0)(currInd)
    point
  }

  /** Returns the interpolated grid indices and their corresponding weights. */
  def interpolants(point: Array[Double]): (Array[Int], Array[Double]) = {
    resetIndexAndWeight()

    var offset = 1
    var subblockSize = 1
    var cuti = 0

    for (d <- point.indices) {
      val coord = point(d)
      val lasti = cutCounts(d) + cuti - 1
      var ii = cuti

      var ilo = ii
      var ihi = ii
      if (coord <= cuts(ii)) {
        // keep above assignment
      } else if (coord >= cuts(lasti)) {
        ilo = lasti
        ihi = lasti
      } else {
        while (cuts(ii) < coord) {
          ii += 1
        }
        if (cuts(ii) == coord) {
          ilo = ii
          ihi = ii
        } else {
          ilo = ii - 1
          ihi = ii
        }
      }

      if (ilo == ihi) {
        for (i <- 0 until offset) {
          index(i) += (ilo - cuti) * subblockSize
        }
      } else {
        val low = 1 - (coord - cuts(ilo)) / (cuts(ihi) - cuts(ilo))
        for (i <- 0 until offset) {
          index2(i) = index(i) + (ilo - cuti) * subblockSize
          index2(i + offset) = index(i) + (ihi - cuti) * subblockSize
        }
        Array.copy(index2, 0, index, 0, index2.length)

        for (i <- 0 until offset) {
          weight2(i) = weight(i) * low
          weight2(i + offset) = weight(i) * (1 - low)
        }
        Array.copy(weight2, 0, weight, 0, weight2.length)

        offset *= 2
      }

      cuti += cutCounts(d)
      subblockSize *= cutCounts(d)
    }

    if (offset < index.length) {
      // no need to interpolate all dimensions because we're on a boundary
      val indices = for (i <- (0 until offset).toArray) yield index(i)
      val weights = for (i <- (0 until offset).toArray) yield weight(i)
      (indices, weights)
    } else {
      (index, weight)
    }
  }

  def resetIndexAndWeight(): Unit = {
    index.indices.foreach { index(_) = 0 }
    weight.indices.foreach { weight(_) = 0.0 }
    weight(0) = 1.0

    index2.indices.foreach { index2(_) = 0 }
    weight2.indices.foreach { weight2(_) = 0.0 }
    weight2(0) = 1.0
  }

  /**
   * Uses the indices and weights from interpolants to obtain an interpolated
   * value from <refValues>.
   */
  def interpolate(point: Array[Double], refValues: Array[Double]): Double = {
    val (indices, weights) = interpolants(point)
    var result = 0.0
    for (iweight <- weights.indices) {
      result += refValues(indices(iweight)) * weights(iweight)
    }
    result
  }
}

object Grid {
  def apply(rawCutPoints: Array[Double]*): Grid = {
    val cutPoints = rawCutPoints.toArray
    val cutCounts = for (i <- cutPoints.indices.toArray) yield cutPoints(i).length
    val cuts = cutPoints.flatten

    val numGridDimensions = cutPoints.length
    val numBoundingPoints = pow(2, numGridDimensions)

    val index = Array.fill[Int](numBoundingPoints)(0)
    val weight = Array.fill[Double](numBoundingPoints)(0.0)
    index(0) = 1
    weight(0) = 1.0

    val index2 = Array.fill[Int](numBoundingPoints)(0)
    val weight2 = Array.fill[Double](numBoundingPoints)(0.0)
    index2(0) = 1
    weight2(0) = 1.0

    new Grid(cutPoints, cutCounts, cuts, index, weight, index2, weight2)
  }
}
