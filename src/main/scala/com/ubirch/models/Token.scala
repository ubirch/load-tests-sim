package com.ubirch.models

import java.util.UUID

import org.json4s.JValue
import org.json4s.JsonAST.JNothing

import scala.util.Try

case class Token(value: String, json: JValue, sub: String, name: String, email: String, roles: List[Symbol]) {
  def id: String = sub
  def ownerId: String = id
  def isAdmin: Boolean = roles.contains(Token.ADMIN)
  def isUser: Boolean = roles.contains(Token.USER)
  def hasRole(role: Symbol): Boolean = roles.contains(role)
  def ownerIdAsUUID: Try[UUID] = Try(UUID.fromString(ownerId))
}

object Token {
  final val ADMIN = 'ADMIN
  final val USER = 'USER
  def apply(value: String): Token = new Token(value, JNothing, sub = "", name = "", email = "", roles = Nil)
}
