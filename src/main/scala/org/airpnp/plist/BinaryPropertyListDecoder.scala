package org.airpnp.plist

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.ArrayList
import java.util.List
import org.airpnp.Util

object BinaryPropertyListDecoder {
  //# HEADER
  //#         magic number ("bplist")
  //#         file format version
  //#
  //# OBJECT TABLE
  //#         variable-sized objects
  //#
  //#         Object Formats (marker byte followed by additional info in some cases)
  //#         null    0000 0000
  //#         bool    0000 1000                       // false
  //#         bool    0000 1001                       // true
  //#         fill    0000 1111                       // fill byte
  //#         int     0001 nnnn       ...             // # of bytes is 2^nnnn, big-endian bytes
  //#         real    0010 nnnn       ...             // # of bytes is 2^nnnn, big-endian bytes
  //#         date    0011 0011       ...             // 8 byte float follows, big-endian bytes
  //#         data    0100 nnnn       [int]   ...     // nnnn is number of bytes unless 1111 then int count follows, followed by bytes
  //#         string  0101 nnnn       [int]   ...     // ASCII string, nnnn is # of chars, else 1111 then int count, then bytes
  //#         string  0110 nnnn       [int]   ...     // Unicode string, nnnn is # of chars, else 1111 then int count, then big-endian 2-byte uint16_t
  //#                 0111 xxxx                       // unused
  //#         uid     1000 nnnn       ...             // nnnn+1 is # of bytes
  //#                 1001 xxxx                       // unused
  //#         array   1010 nnnn       [int]   objref* // nnnn is count, unless '1111', then int count follows
  //#                 1011 xxxx                       // unused
  //#         set     1100 nnnn       [int]   objref* // nnnn is count, unless '1111', then int count follows
  //#         dict    1101 nnnn       [int]   keyref* objref* // nnnn is count, unless '1111', then int count follows
  //#                 1110 xxxx                       // unused
  //#                 1111 xxxx                       // unused
  //#
  //# OFFSET TABLE
  //#         list of ints, byte size of which is given in trailer
  //#         -- these are the byte offsets into the file
  //#         -- number of these is in the trailer
  //#
  //# TRAILER
  //#         byte size of offset ints in offset table
  //#         byte size of object refs in arrays and dicts
  //#         number of offsets in offset table (also is number of objects)
  //#         element # in offset table which is top level object
  //#         offset table offset

  // From CFDate Reference: "Absolute time is measured in seconds relative to
  // the absolute reference date of Jan 1 2001 00:00:00 GMT".
  private val SECS_EPOCH_TO_2001 = 978307200

  private val MARKER_NULL = 0X00
  private val MARKER_FALSE = 0X08
  private val MARKER_TRUE = 0X09
  private val MARKER_FILL = 0X0F
  private val MARKER_INT = 0X10
  private val MARKER_REAL = 0X20
  private val MARKER_DATE = 0X30
  private val MARKER_DATA = 0X40
  private val MARKER_ASCIISTRING = 0X50
  private val MARKER_UNICODE16STRING = 0X60
  private val MARKER_UID = 0X80
  private val MARKER_ARRAY = 0XA0
  private val MARKER_SET = 0XC0
  private val MARKER_DICT = 0XD0

  private val SIGNATURE = "bplist0"

  private class ObjectRef private[plist] (private val index: Int) extends PropertyListObject[Any] {
    private var inner: PropertyListObject[_] = null

    private[plist] def resolve(lst: scala.Array[PropertyListObject[_]]) = inner = lst(index)

    def accept(visitor: PropertyListObjectVisitor) = {
      checkResolved
      inner.accept(visitor)
    }

    def getValue() = {
      checkResolved
      inner.getValue()
    }

    private def checkResolved() = {
      if (inner == null) {
        throw new IllegalStateException("Object reference has not been resolved.")
      }
    }
  }

  private class BinaryPListReader private[plist] (private val buffer: scala.Array[Byte]) {
    private var index = 0
    private val len = buffer.length
    private var objectRefSize = 0
    private var offsets: scala.Array[Long] = null

    private def seek(idx: Int) = index = idx

    private def getString(count: Int) = {
      val idx = index
      index += count
      new java.lang.String(buffer, idx, count)
    }

    private def getUnsignedChar() = {
      val idx = index
      index += 1
      buffer(idx) & 0xFF
    }

    private def readSizedInt(size: Int): Long = {
      // in format version '00', 1, 2, and 4-byte integers have to be
      // interpreted as unsigned, whereas 8-byte integers are signed
      // (and 16-byte when available). negative 1, 2, 4-byte integers
      // are always emitted as 8 bytes in format '00'
      if (!(size == 1 || size == 2 || size == 4 || size == 8)) {
        throw new PropertyListUnhandledException("Unhandled int size: "
          + size)
      }
      (0l /: (0 until size)) { (ret, _) => (ret << 8) | getUnsignedChar }
    }

    private def readSizedFloat(log2count: Int): Double = {
      log2count match {
        // 32 bits
        case 2 => java.lang.Float.intBitsToFloat(readSizedInt(4).asInstanceOf[Int])
        // 64 bits
        case 3 => java.lang.Double.longBitsToDouble(readSizedInt(8))
        case _ => {
          throw new PropertyListUnhandledException(
            "Unhandled real size: " + (1 << log2count))
        }
      }
    }

    private def getUnsignedLongLong(): Long = {
      val ret = readSizedInt(8)
      if (ret < 0) {
        throw new PropertyListUnhandledException(
          "True unsigned long longs not handled yet: " + ret)
      }
      ret
    }

    private def readCount(nb2: Int): Int = {
      nb2 match {
        case 0xf => readObject.getValue.asInstanceOf[Long].intValue
        case _ => nb2
      }
    }

    private def readBytes(count: Int): scala.Array[Byte] = {
      val ret = new scala.Array[Byte](count)
      System.arraycopy(buffer, index, ret, 0, count)
      index += count
      ret
    }

    def read(): PropertyListObject[_] = {
      // Verify the signature the first version digit is always 0.
      seek(0)
      val sig = getString(7)
      if (!sig.equals(SIGNATURE)) {
        throw new PropertyListFormatException("Invalid signature: "
          + sig)
      }

      // Read the trailer (validation omitted for now). Skip first 5 bytes
      // (padding) and sortVersion.
      // typedef struct {
      //   uint8_t _unused[5]
      //   uint8_t _sortVersion
      //   uint8_t _offsetIntSize
      //   uint8_t _objectRefSize
      //   uint64_t _numObjects
      //   uint64_t _topObject
      //   uint64_t _offsetTableOffset
      // } CFBinaryPlistTrailer
      seek(len - 26)
      val offsetIntSize = getUnsignedChar
      objectRefSize = getUnsignedChar
      val numObjects = getUnsignedLongLong.asInstanceOf[Int]
      val topObject = getUnsignedLongLong.asInstanceOf[Int]
      val offsetTableOffset = getUnsignedLongLong.asInstanceOf[Int]

      // Read the object offsets.
      seek(offsetTableOffset)
      offsets = new scala.Array[Long](numObjects)
      (0 until numObjects).foreach(i => offsets(i) = readSizedInt(offsetIntSize))

      // Read the actual objects.
      val objects = new scala.Array[PropertyListObject[_]](offsets.length)
      (0 until numObjects).foreach(i => objects(i) = readObject(offsets(i).asInstanceOf[Int]))

      // Resolve lazy values (references to the object list).
      resolveObjects(objects)

      objects(topObject)
    }

    private def resolveObject(obj: PropertyListObject[_], all: scala.Array[PropertyListObject[_]]) = obj match {
      case x: ObjectRef => x.resolve(all)
      case _ =>
    }

    private def resolveObjects(objects: scala.Array[PropertyListObject[_]]) = {
      // All resolutions are in-place, to avoid breaking references to
      // the outer objects!
      objects.foreach(o => o match {
        case a: Array => {
          a.getValue.foreach(x => resolveObject(x, objects))
        }
        case d: Dict => {
          d.entries.foreach(e => {
            resolveObject(e.getKeyObject, objects)
            resolveObject(e.getValue, objects)
          })
        }
        case _ => resolveObject(o, objects)
      })
    }

    private def readObject(): PropertyListObject[_] = readObject(-1)

    private def readObject(offset: Int): PropertyListObject[_] = {
      var offs = offset
      if (offs >= 0) {
        seek(offs)
      } else {
        offs = index // for the error message
      }
      val marker = getUnsignedChar()
      val nb1 = marker & 0xf0
      val nb2 = marker & 0x0f

      def unknown() = {
        throw new PropertyListFormatException(
          "Unknown marker at position " + offs + ": " + marker)

      }

      nb1 match {
        case MARKER_NULL => {
          marker match {
            case MARKER_NULL => null
            case MARKER_FALSE => False.INSTANCE
            case MARKER_TRUE => True.INSTANCE
            case _ => unknown
          }
        }
        case MARKER_INT => new Integer(readSizedInt(1 << nb2))
        case MARKER_REAL => new Real(readSizedFloat(nb2))
        case MARKER_DATE => {
          assert(nb2 == 3)
          var secs = readSizedFloat(3).asInstanceOf[Long]
          secs += SECS_EPOCH_TO_2001
          new Date(new java.util.Date(secs * 1000))
        }
        case MARKER_DATA => new Data(readBytes(readCount(nb2)))
        case MARKER_ASCIISTRING => {
          val bytes = readBytes(readCount(nb2))
          new String(new java.lang.String(bytes, 0, bytes.length))
        }
        case MARKER_UNICODE16STRING => {
          val count = readCount(nb2)
          val sb = new StringBuilder
          (0 until count).foreach(_ => sb.append(readSizedInt(2).asInstanceOf[Char]))
          new String(sb.toString())
        }
        case MARKER_UID => new Integer(readSizedInt(1 + nb2))
        case MARKER_ARRAY | MARKER_SET => {
          val count = readCount(nb2)
          // We store lazy references to the object list.
          val arr = new scala.Array[PropertyListObject[_]](count)
          for (i <- 0 until count) {
            arr(i) = new BinaryPropertyListDecoder.ObjectRef(readSizedInt(objectRefSize).asInstanceOf[Int])
          }
          new Array(arr: _*)
        }
        case MARKER_DICT => {
          val count = readCount(nb2)
          // First N keys, then N values.
          // We store lazy references to the object list.
          val keys = new scala.Array[PropertyListObject[_]](count)
          for (i <- 0 until count) {
            keys(i) = new BinaryPropertyListDecoder.ObjectRef(readSizedInt(objectRefSize).asInstanceOf[Int])
          }
          val values = new scala.Array[PropertyListObject[_]](count)
          for (i <- 0 until count) {
            values(i) = new BinaryPropertyListDecoder.ObjectRef(readSizedInt(objectRefSize).asInstanceOf[Int])
          }
          new Dict(keys.zip(values).map(t => new KeyValue(t._1, t._2)): _*)
        }
        case _ => unknown
      }
    }
  }

}

/**
 * Reads an object from a binary property list.
 *
 * The binary property list format is described in CFBinaryPList.c at
 * http://opensource.apple.com/source/CF/CF-550/CFBinaryPList.c. Only the top
 * level object is returned.
 *
 * Throws a PListFormatError or a PListUnhandledError if the input data cannot be
 * fully understood.
 *
 * Arguments: is -- an input stream
 */
class BinaryPropertyListDecoder(private val is: InputStream) {
  private val buffer = Util.readAllBytes(is)

  def decode(): PropertyList = {
    val root = new BinaryPropertyListDecoder.BinaryPListReader(buffer).read()
    new PropertyList(root)
  }
}
