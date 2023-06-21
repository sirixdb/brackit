<?xml version="1.0"?>

<!--
  An XSLT that can be used to sort formatting configuration files' contents.
  Sorting makes comparisons between IntelliJ outputs and Eclipse outputs easier.

  Example use: xsltproc -o brackit-formatter-out.xml sort.xsl brackit-formatter.xml
-->

<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform" xmlns:xs="http://www.w3.org/2001/XMLSchema"
  xmlns:func="http://exslt.org/functions" xmlns:local="http://brackit.io" extension-element-prefixes="func"
  version="1.0">

  <xsl:output encoding="utf-8" method="xml" omit-xml-declaration="yes" indent="yes"/>
  <xsl:strip-space elements="*"/>

  <xsl:template match="@*|node()">
    <xsl:copy>
      <xsl:apply-templates select="@*">
        <xsl:sort select="local-name()"/>
        <xsl:sort select="."/>
      </xsl:apply-templates>
      <xsl:apply-templates select="node()">
        <xsl:sort select="local-name()"/>
        <xsl:sort select="local:key2(.)"/>
        <xsl:sort select="local:key3(.)"/>
        <xsl:sort select="." data-type="number"/>
      </xsl:apply-templates>
    </xsl:copy>
  </xsl:template>

  <func:function name="local:key2">
    <xsl:param name="e" select="."/>
    <func:result>
      <xsl:for-each select="$e/@*">
        <xsl:sort select="local-name()"/>
        <xsl:sort select="string()"/>
        <xsl:value-of select="concat(local-name(), ' ')"/>
      </xsl:for-each>
    </func:result>
  </func:function>

  <func:function name="local:key3">
    <xsl:param name="e" select="."/>
    <func:result>
      <xsl:for-each select="$e/@*">
        <xsl:sort select="local-name()"/>
        <xsl:sort select="string()"/>
        <xsl:value-of select="concat(string(), ' ')"/>
      </xsl:for-each>
    </func:result>
  </func:function>

</xsl:stylesheet>
