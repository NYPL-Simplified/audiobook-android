package org.librarysimplified.audiobook.lcp.license_status

import org.librarysimplified.audiobook.parser.api.ParserProviderType

/**
 * A provider of license status document parsers.
 */

interface LicenseStatusParserProviderType :
  ParserProviderType<ByteArray, Any, LicenseStatusDocument>
