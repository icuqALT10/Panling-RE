package icu.icuqalt10.panlingre.init;

import com.mojang.serialization.Codec;
import icu.icuqalt10.panlingre.PanlingRE;
import icu.icuqalt10.panlingre.attachment.BlessData;
import icu.icuqalt10.panlingre.attachment.LingQiData;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

public class ModAttachments {

    public static final DeferredRegister<AttachmentType<?>> ATTACHMENT_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.ATTACHMENT_TYPES, PanlingRE.MODID);

    public static final Supplier<AttachmentType<LingQiData>> LINGQI = ATTACHMENT_TYPES.register(
            "lingqi",
            () -> AttachmentType.builder(() -> new LingQiData(20.0f))
                    .serialize(Codec.FLOAT.xmap(LingQiData::new, LingQiData::getCurrent))
                    .copyOnDeath()
                    .build()
    );

    public static final Supplier<AttachmentType<BlessData>> BLESS = ATTACHMENT_TYPES.register(
            "bless",
            () -> AttachmentType.builder(BlessData::new)
                    .serialize(BlessData.CODEC)
                    .copyOnDeath()
                    .build()
    );

}
