package quantum.video.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Follow record encapsulates configuration settings for follow operations.
 * Contains references to primary configuration, customer information,
 * and various service endpoints needed for user authentication flow.
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record Follow(
        /**
         * Primary configuration settings
         */
        Primary primary,

        /**
         * Customer identifier
         */
        String customerId,

        /**
         * External identifier for integration with other systems
         */
        String externalId,

        /**
         * Analytics configuration settings
         */
        Analytics analytics,

        /**
         * PRM (Provider Resource Management) host value configuration
         */
        HostValue prm,

        /**
         * Resource service identifier
         */
        String rs,

        /**
         * MSCS (Media Services Control System) host value configuration
         */
        HostValue mscs,

        /**
         * Localization identifier for regional settings
         */
        String localizationId,

        /**
         * Flag indicating whether purchases are allowed
         */
        boolean purchaseAllowed
) {

    /**
     * Default constructor that initializes all fields with default values.
     * Creates new instances of Primary, Analytics, and HostValue with default configurations.
     */
    public Follow() {
        this(
            new Primary(1111),
            "",
            "",
            new Analytics (5, 1, "","", "", 180, "", ""),
            new HostValue(""),
            "",
            new HostValue(""),
            "1",
            true
        );
    }
}
