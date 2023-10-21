package dev.sergiferry.languages.api.location;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang.Validate;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;

import java.net.URL;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

/**
 * Creado por SergiFerry el 09/09/2022
 */
public class NationalFlag {

    private static final String TEXTURE_BASE64_PATTERN = "{\"textures\":{\"SKIN\":{\"url\":\"%s\"}}}";
    private static final String TEXTURE_MINECRAFT_PATTERN = "http://textures.minecraft.net/texture/%s";

    private String headTextureID;
    private List<Pattern> bannerPatterns;

    protected NationalFlag() {}

    public boolean isSettledHeadTexture() { return this.headTextureID != null; }

    public String getHeadTextureID() { return headTextureID; }

    public String getHeadTextureURL() { return String.format(TEXTURE_MINECRAFT_PATTERN, headTextureID); }

    public String getHeadTexture() { return String.format(TEXTURE_BASE64_PATTERN, getHeadTextureURL()); }

    protected void setHeadTextureID(String headTextureID) { this.headTextureID = headTextureID; }

    public List<Pattern> getBannerPatterns() { return bannerPatterns; }

    public void setBannerPatterns(List<Pattern> bannerPatterns) { this.bannerPatterns = bannerPatterns; }

    public static Builder builder(){ return new Builder(); }

    public static class Builder{

        private NationalFlag nationalFlag;

        private Builder() { this.nationalFlag = new NationalFlag(); }

        public Builder withHeadTexture(String texture){
            if(texture.length() == 64){
                nationalFlag.setHeadTextureID(texture);
                return this;
            }
            else if(texture.startsWith(String.format(TEXTURE_MINECRAFT_PATTERN, ""))){
                nationalFlag.setHeadTextureID(texture.replaceFirst(String.format(TEXTURE_MINECRAFT_PATTERN, ""), ""));
                return this;
            }
            byte[] decodedBytes = Base64.getDecoder().decode(texture);
            String decodedTexture = new String(decodedBytes);
            if(decodedTexture.contains("SKIN")){
                JsonObject textureJSON = JsonParser.parseString(decodedTexture).getAsJsonObject();
                JsonObject textureElement = textureJSON.get("textures").getAsJsonObject();
                String textureURL = textureElement.get("SKIN").getAsJsonObject().get("url").getAsString();
                nationalFlag.setHeadTextureID(textureURL.replaceFirst(String.format(TEXTURE_MINECRAFT_PATTERN, ""), ""));
            }
            throw new IllegalArgumentException("This texture \"" + texture + "\" is not valid");
        }

        public Builder withBanner(ItemStack itemStack){
            Validate.isTrue(itemStack != null);
            Validate.isTrue(itemStack.hasItemMeta());
            Validate.isTrue(itemStack.getItemMeta() instanceof BannerMeta);
            nationalFlag.setBannerPatterns(((BannerMeta) itemStack.getItemMeta()).getPatterns());
            return this;
        }

        public Builder withBanner(BannerMeta bannerMeta){
            nationalFlag.setBannerPatterns(bannerMeta.getPatterns());
            return this;
        }

        public Builder withBanner(Pattern... pattern){
            nationalFlag.setBannerPatterns(Arrays.stream(pattern).toList());
            return this;
        }

        public NationalFlag build(){ return this.nationalFlag; }

    }
}
