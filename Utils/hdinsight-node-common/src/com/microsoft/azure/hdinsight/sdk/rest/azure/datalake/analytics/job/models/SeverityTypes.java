/**
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Defines values for SeverityTypes.
 */
public enum SeverityTypes {
    /** Enum value Warning. */
    WARNING("Warning"),

    /** Enum value Error. */
    ERROR("Error"),

    /** Enum value Info. */
    INFO("Info"),

    /** Enum value SevereWarning. */
    SEVERE_WARNING("SevereWarning"),

    /** Enum value Deprecated. */
    DEPRECATED("Deprecated"),

    /** Enum value UserWarning. */
    USER_WARNING("UserWarning");

    /** The actual serialized value for a SeverityTypes instance. */
    private String value;

    SeverityTypes(String value) {
        this.value = value;
    }

    /**
     * Parses a serialized value to a SeverityTypes instance.
     *
     * @param value the serialized value to parse.
     * @return the parsed SeverityTypes object, or null if unable to parse.
     */
    @JsonCreator
    public static SeverityTypes fromString(String value) {
        SeverityTypes[] items = SeverityTypes.values();
        for (SeverityTypes item : items) {
            if (item.toString().equalsIgnoreCase(value)) {
                return item;
            }
        }
        return null;
    }

    @JsonValue
    @Override
    public String toString() {
        return this.value;
    }
}
