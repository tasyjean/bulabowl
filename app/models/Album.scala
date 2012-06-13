package models

import play.api.db._
import anorm._
import play.api.Play.current


import anorm.SqlParser._


/**
 * Created by IntelliJ IDEA.
 * User: Keyston
 * Date: 6/4/12
 * Time: 1:11 PM
 */

import java.sql.Date
import play.api.db._
import play.api.Play.current

// Import the session management, including the implicit threadLocalSession

import org.scalaquery.session._


// Import the query language

import org.scalaquery.ql._

// Import the standard SQL types

import org.scalaquery.ql.TypeMapper._

// Use H2Driver which implements ExtendedProfile and thus requires ExtendedTables


import org.scalaquery.ql.extended.H2Driver.Implicit._
import org.scalaquery.ql.extended.{ExtendedTable => Table}


case class Album(id: Long, artistID: Long, name: String, artistName: Option[String], slug: String = "", public: Boolean = true, active: Boolean = false,
                 download: Boolean = true, donateMore: Boolean = true, price: Double = 7.00,
                 art: Option[String], about: Option[String], credits: Option[String], upc: Option[String], releaseDate: Option[Date])
{

}


object Albums extends Table[Album]("albums") with DataTable
{

  def id = column[Long]("id", O.PrimaryKey, O AutoInc)

  def artistID = column[Long]("artis_id")

  def name = column[String]("name", O.NotNull)

  def slug = column[String]("slug")

  def artistName = column[Option[String]]("artistName", O.Nullable, O DBType ("varchar(45)"))

  def public = column[Boolean]("public", O Default (true))

  def active = column[Boolean]("active", O Default (false))

  def download = column[Boolean]("download", O Default (true))


  def donateMore = column[Boolean]("donateMore", O Default (true))

  def price = column[Double]("price")

  def art = column[Option[String]]("art", O.Nullable, O DBType ("varchar(45)"))

  def about = column[Option[String]]("about", O.Nullable, O DBType ("text"))

  def credits = column[Option[String]]("credits", O.Nullable, O DBType ("text"))

  def upc = column[Option[String]]("upc", O.Nullable, O DBType ("varchar(20)"))


  def releaseDate = column[Option[Date]]("releaseDate", O Nullable)


  def * = id ~ artistID ~ name ~ artistName ~ slug ~ public ~ active ~ download ~ donateMore ~ price ~ art ~ about ~ credits ~ upc ~ releaseDate <>(Album.apply _, Album.unapply _)


  def bySlug(artistId: Long, slug: String): Option[Album] =
  {
    db withSession {
      implicit s =>
        (for {
          a <- Albums if a.artistID === artistId && a.name === slug.bind
        } yield a).firstOption
    }
  }

  def validateSlug(slug: String): Boolean =
  {
    db withSession {
      implicit s =>

        val count = (for {
          a <- Albums if a.slug === slug.bind
        } yield a.slug.count).first
        count != 0
    }
  }

  def withTracks(albumID: Long): List[Track] =
  {
    db withSession {
      implicit s =>
        (for {
          a <- AlbumTracks if a.albumID === albumID
          t <- AlbumTracks.tracks
          _ <- Query orderBy a.trackOrder
        } yield t).list
    }
  }


}

case class Track(id: Long, artistID: Long, name: String, download: Boolean = true, price: Double = 7.00,
                 artistName: Option[String],
                 art: Option[String], lyrics: Option[String], about: Option[String], credits: Option[String], isrc: Option[String], releaseDate: Option[Date])


object Tracks extends Table[Track]("tracks") with DataTable
{


  def artistID = column[Long]("artist_id")

  def download = column[Boolean]("download")


  def lyrics = column[Option[String]]("lyrics", O.Nullable, O DBType ("text"))


  def isrc = column[Option[String]]("isrc", O.Nullable, O DBType ("varchar(10)"))

  def id = column[Long]("id", O.PrimaryKey, O.AutoInc)

  def name = column[String]("name", O.NotNull)

  def about = column[Option[String]]("about", O.Nullable, O DBType ("text"))

  def price = column[Double]("price")

  def credits = column[Option[String]]("credits", O.Nullable, O DBType ("text"))

  def artistName = column[Option[String]]("artistName", O.Nullable, O DBType ("varchar(45)"))

  def art = column[Option[String]]("art", O.Nullable, O DBType ("varchar(45)"))

  def releaseDate = column[Option[Date]]("releaseDate", O Nullable)


  def * = id ~ artistID ~ name ~ download ~ price ~ artistName ~ art ~ lyrics ~ about ~ credits ~ isrc ~ releaseDate <>(Track.apply _, Track.unapply _)

  def noID = artistID ~ name ~ download ~ price ~ artistName ~ art ~ lyrics ~ about ~ credits ~ isrc ~ releaseDate


}

case class AlbumTracks(albumID: Long, trackID: Long, order: Int)

object AlbumTracks extends Table[AlbumTracks]("album_tracks") with DataTable
{


  def albumID = column[Long]("album_id")

  def trackID = column[Long]("track_id")

  def trackOrder = column[Int]("trackOrder")

  def fkAlbum = foreignKey("fk_album", albumID, Albums)(_.id)

  def fkTrack = foreignKey("fk_track", trackID, Tracks)(_.id)

  def * = albumID ~ trackID ~ trackOrder <>(AlbumTracks.apply _, AlbumTracks.unapply _)

  def albumTrackIndex = index("idx_album_track", albumID ~ trackID, unique = true)

  def tracks = Tracks.where(_.id === trackID)


}






