package org.librarysimplified.audiobook.tests.local

import org.librarysimplified.audiobook.lcp.license_status.LicenseStatusParserProviderType
import org.librarysimplified.audiobook.lcp.license_status.LicenseStatusParsers
import org.librarysimplified.audiobook.tests.LicenseStatusParserContract

class LicenseStatusParserTest : LicenseStatusParserContract() {
  override fun parsers(): LicenseStatusParserProviderType {
    return LicenseStatusParsers
  }
}
