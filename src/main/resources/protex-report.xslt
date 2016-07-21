<!-- 
Protex Plugin Integration
Copyright (C) 2015 Black Duck Software, Inc.

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 2
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
-->
<?xml version="1.0"?>
<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
  <xsl:output method="xml" />
  <xsl:output omit-xml-declaration="yes" />
  <xsl:output version="1.1" />

  <!-- Remove the date header -->
  <xsl:template match="/html/body/div[@class = 'reportHeader' and descendant::h4]" />

  <!-- Reduce H1 to H2 for Maven reports -->
  <xsl:template match="/html/body/div[@class = 'reportHeader']/h1">
    <xsl:element name="h2">
      <xsl:apply-templates select="text()"/>
    </xsl:element>  
  </xsl:template>

  <!-- If the report contains a table with a message row, just show the message -->
  <!-- NOTE we are looking for a colspan of 2 because the analysis summary report is messed up -->
  <xsl:template match="/html/body/table[@class = 'reportTable' and tbody/tr/td[1]/@colspan > 2]">
    <xsl:element name="p">
      <xsl:value-of select="tbody/tr/td[1]/text()" />
    </xsl:element>
  </xsl:template>

  <!-- Remove the extra header row that duplicates the H2 header -->
  <xsl:template match="/html/body/table[@class = 'reportTable']/thead/tr[1]" />

  <!-- Replace the Protex 'reportTable' CSS class with the Maven 'bodyTable' -->
  <xsl:template match="/html/body/table/@class[. = 'reportTable']">
    <xsl:attribute name="class">bodyTable</xsl:attribute>
  </xsl:template>

  <!-- Copy EVERYTHING else (except what we explicitly match below) -->
  <xsl:template match="@*|node()|text()|comment()|processing-instruction()">
    <xsl:copy>
      <xsl:apply-templates select="@*|node()|text()|comment()|processing-instruction()"/>
    </xsl:copy>
  </xsl:template>

  <!-- Remove garbage styling attributes -->
  <xsl:template match="@style" />
  <xsl:template match="@align" />
  <xsl:template match="@valign" />
  <xsl:template match="@bgcolor" />
  <xsl:template match="@cellspacing" />
  <xsl:template match="@cellpadding" />
  
  <!-- Fix upper case <I> tags used for highlighting stuff in the reports -->
  <xsl:template match="I">
    <xsl:element name="span">
      <xsl:attribute name="class">mark</xsl:attribute>
      <xsl:apply-templates />
    </xsl:element>
  </xsl:template>

</xsl:stylesheet>