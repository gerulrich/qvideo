package quantum.video.client;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

/**
 * Follow record encapsulates configuration settings for follow operations.
 * Contains references to primary configuration, customer information,
 * and various service endpoints needed for user authentication flow.
 *
 * @param primary Primary configuration settings
 * @param customerId Customer identifier
 * @param externalId External identifier for integration with other systems
 * @param analytics Analytics configuration settings
 * @param prm PRM (Provider Resource Management) host value configuration
 * @param rs Resource service identifier
 * @param mscs MSCS (Media Services Control System) host value configuration
 * @param localizationId Localization identifier for regional settings
 * @param purchaseAllowed Flag indicating whether purchases are allowed
 */
@JsonNaming(PropertyNamingStrategies.LowerCamelCaseStrategy.class)
public record Follow(
        Primary primary,
        String customerId,
        String externalId,
        Analytics analytics,
        HostValue prm,
        String rs,
        HostValue mscs,
        String localizationId,
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
