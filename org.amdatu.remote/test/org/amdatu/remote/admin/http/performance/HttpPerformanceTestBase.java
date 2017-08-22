/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.amdatu.remote.admin.http.performance;

import static java.net.HttpURLConnection.HTTP_OK;

import java.util.ArrayList;
import java.util.List;

import org.amdatu.remote.admin.http.HttpEndpointTestBase;
import org.apache.commons.lang3.time.StopWatch;

public abstract class HttpPerformanceTestBase extends HttpEndpointTestBase {

    protected static String m_bigStr =
        "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Vivamus aliquam aliquet risus. Vestibulum ultricies rhoncus tincidunt. Proin pellentesque eros quis quam dictum, nec porttitor purus sollicitudin. Sed a orci sed risus euismod condimentum nec eget sapien. Cum sociis natoque penatibus et magnis dis parturient montes, nascetur ridiculus mus. Fusce ornare purus at turpis mattis, sed varius odio sodales. Interdum et malesuada fames ac ante ipsum primis in faucibus. Pellentesque varius maximus sodales. Donec aliquam quam eget porttitor vestibulum. Morbi cursus fringilla justo, sed interdum sem placerat at. In consequat id odio nec aliquam.Curabitur id sollicitudin arcu, ut facilisis tellus. Quisque volutpat ac lectus id mollis. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Nam rhoncus posuere pulvinar. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Suspendisse lacinia porttitor arcu sit amet scelerisque. Quisque dictum elementum risus sit amet maximus. Fusce rutrum faucibus sem, eget vulputate turpis. Etiam dapibus sodales neque et tincidunt.Integer nulla libero, interdum ut arcu eget, convallis iaculis turpis. Praesent hendrerit risus ultrices metus faucibus sagittis. Morbi pharetra sagittis odio. Fusce nec euismod augue. Duis tempor ex nulla, blandit efficitur diam condimentum non. Ut vel pharetra nisl, sed mollis massa. Nam maximus risus vitae eros fermentum sodales. Donec neque dui, dapibus a lorem vitae, blandit mollis diam. Aliquam in massa felis.Phasellus vel risus ac lacus tempus vulputate non in erat. Praesent ut sapien a nulla consectetur maximus. In varius nisi odio, ac fringilla lacus ultrices ut. Phasellus luctus id enim at ultrices. Aliquam vitae scelerisque purus. Praesent scelerisque eu purus eu fringilla. Phasellus dignissim metus eu est pulvinar iaculis. Class aptent taciti sociosqu ad litora torquent per conubia nostra, per inceptos himenaeos. Praesent metus dui, elementum id turpis accumsan, mollis molestie sapien. Morbi eget volutpat tellus. Donec quis quam ut massa lobortis scelerisque. Fusce vehicula odio non est elementum, eu vehicula neque accumsan. In accumsan vel velit eget semper.Quisque ac erat ante. Nullam imperdiet enim leo. Aliquam nec justo vitae ipsum porta mattis. Phasellus sit amet arcu eros. Morbi eget aliquam quam, vitae tempus leo. Nulla pretium massa in porta imperdiet. Donec eu hendrerit neque, quis auctor diam. Mauris venenatis libero sit amet arcu volutpat condimentum vitae sit amet mi. Proin hendrerit accumsan facilisis. Nam erat nibh, pretium nec consectetur ut, ornare in arcu. Ut luctus accumsan dolor nec lobortis. Sed malesuada massa orci, vel porttitor orci molestie eget. Curabitur at sapien eu orci vulputate ullamcorper at non nibh. Ut non est risus.Mauris et commodo enim. Praesent nisi nisi, tempor eget imperdiet quis, vestibulum nec neque. Suspendisse feugiat nunc quam. Quisque iaculis volutpat arcu, non dignissim velit consectetur in. Integer vulputate accumsan tellus vel luctus. Praesent egestas metus tortor, tempor elementum urna malesuada quis. Maecenas vitae purus sit amet nibh laoreet pharetra. Mauris diam eros, gravida at hendrerit vel, mattis et mi. Ut a pretium ante, sit amet laoreet metus. Duis id sapien vehicula, imperdiet ex id, consequat turpis. Integer finibus id diam laoreet imperdiet. Aenean rhoncus, nulla ut gravida lacinia, felis ipsum fermentum ex, a pulvinar ligula nisi et libero.Mauris ornare accumsan lectus non faucibus. Pellentesque convallis urna quis leo eleifend, eu euismod nulla hendrerit. Phasellus volutpat eros lacus, et auctor purus pellentesque euismod. Morbi viverra cursus nibh vitae pellentesque. Sed vitae laoreet tortor. Etiam a elit ipsum. Fusce est ante, pharetra ut urna in, viverra pretium dui. Proin congue neque sed mi porta rutrum feugiat eu urna. Aenean commodo eros quam, non lacinia lectus mattis eu.Vivamus quis elementum eros. Sed luctus enim libero, non ultricies sem tempor sed. Phasellus eget euismod tellus, ut luctus ligula. Phasellus sit amet arcu aliquet, egestas tortor vel, maximus ipsum. Sed et justo maximus ipsum aliquam posuere malesuada ac urna. Donec eleifend ligula sapien. Aenean lobortis, nisi ac facilisis lobortis, orci libero dictum justo, vestibulum vulputate tortor nunc ut leo. Nam suscipit nibh nibh, quis viverra purus gravida nec. Duis magna ipsum, consectetur ut nibh ut, blandit congue leo. Vivamus orci sem, pharetra sit amet odio vitae, tempor semper enim. Aenean sit amet risus vulputate, iaculis lorem in, tincidunt nunc. Praesent lectus ligula, bibendum vitae euismod vitae, feugiat id mi. Sed vel commodo odio. Cras eget elit posuere, volutpat nisi ac, dictum risus. Nullam leo orci, cursus ac turpis ut, pretium pharetra elit.Aenean rutrum, sem nec accumsan tincidunt, augue nulla cursus nibh, eget luctus orci quam eu urna. Suspendisse ac arcu quam. Vestibulum ante ipsum primis in faucibus orci luctus et ultrices posuere cubilia Curae; Vestibulum sit amet odio volutpat, semper nibh ac, gravida dui. Vestibulum ornare congue nisl, et scelerisque nisi gravida sed. Aliquam consequat nisi ipsum, eget rhoncus eros sagittis non. Ut quis purus elit. Ut sit amet eleifend augue, ac consequat diam. Pellentesque molestie enim nulla, eget finibus justo ullamcorper eu. Vestibulum nec lacinia turpis. Duis euismod dignissim lectus eget tempor. Phasellus vel diam sagittis, aliquam libero vitae, maximus ipsum. Phasellus iaculis gravida dui, mattis tristique mauris placerat nec. Phasellus libero mauris, hendrerit volutpat velit non, elementum vestibulum est. Proin ex quam, finibus non mauris sagittis, egestas placerat eros.Duis sagittis libero vel sem malesuada, vitae rhoncus massa iaculis. Proin ut neque mi. Nullam sit amet nunc at felis bibendum dapibus vehicula eget nulla. Sed elementum suscipit nisl, vitae condimentum mauris. Integer vel nibh a eros placerat sollicitudin a in metus. Donec ut sapien dui. Lorem ipsum dolor sit amet, consectetur adipiscing elit. Integer auctor sed tortor vitae rhoncus. Maecenas euismod et metus id laoreet. Fusce semper dui vitae pretium suscipit. Mauris sodales tellus sit amet erat vestibulum consectetur. Nam tempor egestas semper. Donec ut lectus lacinia, blandit est id, tristique eros. Sed eleifend leo mollis faucibus sollicitudin. Vestibulum mollis pretium facilisis. Nunc fringilla elementum vulputate.Praesent mattis, lorem at ultrices suscipit, est quam commodo nisi, ut suscipit mauris lorem at velit. Vestibulum eu nisl eu nibh venenatis ornare. Curabitur iaculis tempus odio ac lobortis. Maecenas at facilisis lectus. Morbi consequat consectetur arcu sit amet molestie. Donec vel felis ut eros porttitor imperdiet et eget ex. Curabitur eget ante.";
    protected static String m_bigStrFlipped =
        "˙ǝʇuɐ ʇǝbǝ ɹnʇıqɐɹnɔ ˙xǝ ʇǝbǝ ʇǝ ʇǝıpɹǝdɯı ɹoʇıʇʇɹod soɹǝ ʇn sıןǝɟ ןǝʌ ɔǝuop ˙ǝıʇsǝןoɯ ʇǝɯɐ ʇıs nɔɹɐ ɹnʇǝʇɔǝsuoɔ ʇɐnbǝsuoɔ ıqɹoɯ ˙snʇɔǝן sısıןıɔɐɟ ʇɐ sɐuǝɔǝɐɯ ˙sıʇɹoqoן ɔɐ oıpo sndɯǝʇ sıןnɔɐı ɹnʇıqɐɹnɔ ˙ǝɹɐuɹo sıʇɐuǝuǝʌ ɥqıu nǝ ןsıu nǝ ɯnןnqıʇsǝʌ ˙ʇıןǝʌ ʇɐ ɯǝɹoן sıɹnɐɯ ʇıdıɔsns ʇn 'ısıu opoɯɯoɔ ɯɐnb ʇsǝ 'ʇıdıɔsns sǝɔıɹʇןn ʇɐ ɯǝɹoן 'sıʇʇɐɯ ʇuǝsǝɐɹd˙ǝʇɐʇndןnʌ ɯnʇuǝɯǝןǝ ɐןןıbuıɹɟ ɔunu ˙sısıןıɔɐɟ ɯnıʇǝɹd sıןןoɯ ɯnןnqıʇsǝʌ ˙uıpnʇıɔıןןos snqıɔnɐɟ sıןןoɯ oǝן puǝɟıǝןǝ pǝs ˙soɹǝ ǝnbıʇsıɹʇ 'pı ʇsǝ ʇıpuɐןq 'ɐıuıɔɐן snʇɔǝן ʇn ɔǝuop ˙ɹǝdɯǝs sɐʇsǝbǝ ɹodɯǝʇ ɯɐu ˙ɹnʇǝʇɔǝsuoɔ ɯnןnqıʇsǝʌ ʇɐɹǝ ʇǝɯɐ ʇıs snןןǝʇ sǝןɐpos sıɹnɐɯ ˙ʇıdıɔsns ɯnıʇǝɹd ǝɐʇıʌ ınp ɹǝdɯǝs ǝɔsnɟ ˙ʇǝǝɹoɐן pı snʇǝɯ ʇǝ poɯsınǝ sɐuǝɔǝɐɯ ˙snɔuoɥɹ ǝɐʇıʌ ɹoʇɹoʇ pǝs ɹoʇɔnɐ ɹǝbǝʇuı ˙ʇıןǝ buıɔsıdıpɐ ɹnʇǝʇɔǝsuoɔ 'ʇǝɯɐ ʇıs ɹoןop ɯnsdı ɯǝɹoן ˙ınp uǝıdɐs ʇn ɔǝuop ˙snʇǝɯ uı ɐ uıpnʇıɔıןןos ʇɐɹǝɔɐןd soɹǝ ɐ ɥqıu ןǝʌ ɹǝbǝʇuı ˙sıɹnɐɯ ɯnʇuǝɯıpuoɔ ǝɐʇıʌ 'ןsıu ʇıdıɔsns ɯnʇuǝɯǝןǝ pǝs ˙ɐןןnu ʇǝbǝ ɐןnɔıɥǝʌ snqıdɐp ɯnpuǝqıq sıןǝɟ ʇɐ ɔunu ʇǝɯɐ ʇıs ɯɐןןnu ˙ıɯ ǝnbǝu ʇn uıoɹd ˙sıןnɔɐı ɐssɐɯ snɔuoɥɹ ǝɐʇıʌ 'ɐpɐnsǝןɐɯ ɯǝs ןǝʌ oɹǝqıן sıʇʇıbɐs sınp˙soɹǝ ʇɐɹǝɔɐןd sɐʇsǝbǝ 'sıʇʇıbɐs sıɹnɐɯ uou snqıuıɟ 'ɯɐnb xǝ uıoɹd ˙ʇsǝ ɯnןnqıʇsǝʌ ɯnʇuǝɯǝןǝ 'uou ʇıןǝʌ ʇɐdʇnןoʌ ʇıɹǝɹpuǝɥ 'sıɹnɐɯ oɹǝqıן snןןǝsɐɥd ˙ɔǝu ʇɐɹǝɔɐןd sıɹnɐɯ ǝnbıʇsıɹʇ sıʇʇɐɯ 'ınp ɐpıʌɐɹb sıןnɔɐı snןןǝsɐɥd ˙ɯnsdı snɯıxɐɯ 'ǝɐʇıʌ oɹǝqıן ɯɐnbıןɐ 'sıʇʇıbɐs ɯɐıp ןǝʌ snןןǝsɐɥd ˙ɹodɯǝʇ ʇǝbǝ snʇɔǝן ɯıssıubıp poɯsınǝ sınp ˙sıdɹnʇ ɐıuıɔɐן ɔǝu ɯnןnqıʇsǝʌ ˙nǝ ɹǝdɹoɔɯɐןןn oʇsnظ snqıuıɟ ʇǝbǝ 'ɐןןnu ɯıuǝ ǝıʇsǝןoɯ ǝnbsǝʇuǝןןǝd ˙ɯɐıp ʇɐnbǝsuoɔ ɔɐ 'ǝnbnɐ puǝɟıǝןǝ ʇǝɯɐ ʇıs ʇn ˙ʇıןǝ snɹnd sınb ʇn ˙uou sıʇʇıbɐs soɹǝ snɔuoɥɹ ʇǝbǝ 'ɯnsdı ısıu ʇɐnbǝsuoɔ ɯɐnbıןɐ ˙pǝs ɐpıʌɐɹb ısıu ǝnbsıɹǝןǝɔs ʇǝ 'ןsıu ǝnbuoɔ ǝɹɐuɹo ɯnןnqıʇsǝʌ ˙ınp ɐpıʌɐɹb 'ɔɐ ɥqıu ɹǝdɯǝs 'ʇɐdʇnןoʌ oıpo ʇǝɯɐ ʇıs ɯnןnqıʇsǝʌ ؛ǝɐɹnɔ ɐıןıqnɔ ǝɹǝnsod sǝɔıɹʇןn ʇǝ snʇɔnן ıɔɹo snqıɔnɐɟ uı sıɯıɹd ɯnsdı ǝʇuɐ ɯnןnqıʇsǝʌ ˙ɯɐnb nɔɹɐ ɔɐ ǝssıpuǝdsns ˙ɐuɹn nǝ ɯɐnb ıɔɹo snʇɔnן ʇǝbǝ 'ɥqıu snsɹnɔ ɐןןnu ǝnbnɐ 'ʇunpıɔuıʇ uɐsɯnɔɔɐ ɔǝu ɯǝs 'ɯnɹʇnɹ uɐǝuǝɐ˙ʇıןǝ ɐɹʇǝɹɐɥd ɯnıʇǝɹd 'ʇn sıdɹnʇ ɔɐ snsɹnɔ 'ıɔɹo oǝן ɯɐןןnu ˙snsıɹ ɯnʇɔıp 'ɔɐ ısıu ʇɐdʇnןoʌ 'ǝɹǝnsod ʇıןǝ ʇǝbǝ sɐɹɔ ˙oıpo opoɯɯoɔ ןǝʌ pǝs ˙ıɯ pı ʇɐıbnǝɟ 'ǝɐʇıʌ poɯsınǝ ǝɐʇıʌ ɯnpuǝqıq 'ɐןnbıן snʇɔǝן ʇuǝsǝɐɹd ˙ɔunu ʇunpıɔuıʇ 'uı ɯǝɹoן sıןnɔɐı 'ǝʇɐʇndןnʌ snsıɹ ʇǝɯɐ ʇıs uɐǝuǝɐ ˙ɯıuǝ ɹǝdɯǝs ɹodɯǝʇ 'ǝɐʇıʌ oıpo ʇǝɯɐ ʇıs ɐɹʇǝɹɐɥd 'ɯǝs ıɔɹo snɯɐʌıʌ ˙oǝן ǝnbuoɔ ʇıpuɐןq 'ʇn ɥqıu ʇn ɹnʇǝʇɔǝsuoɔ 'ɯnsdı ɐubɐɯ sınp ˙ɔǝu ɐpıʌɐɹb snɹnd ɐɹɹǝʌıʌ sınb 'ɥqıu ɥqıu ʇıdıɔsns ɯɐu ˙oǝן ʇn ɔunu ɹoʇɹoʇ ǝʇɐʇndןnʌ ɯnןnqıʇsǝʌ 'oʇsnظ ɯnʇɔıp oɹǝqıן ıɔɹo 'sıʇɹoqoן sısıןıɔɐɟ ɔɐ ısıu 'sıʇɹoqoן uɐǝuǝɐ ˙uǝıdɐs ɐןnbıן puǝɟıǝןǝ ɔǝuop ˙ɐuɹn ɔɐ ɐpɐnsǝןɐɯ ǝɹǝnsod ɯɐnbıןɐ ɯnsdı snɯıxɐɯ oʇsnظ ʇǝ pǝs ˙ɯnsdı snɯıxɐɯ 'ןǝʌ ɹoʇɹoʇ sɐʇsǝbǝ 'ʇǝnbıןɐ nɔɹɐ ʇǝɯɐ ʇıs snןןǝsɐɥd ˙ɐןnbıן snʇɔnן ʇn 'snןןǝʇ poɯsınǝ ʇǝbǝ snןןǝsɐɥd ˙pǝs ɹodɯǝʇ ɯǝs sǝıɔıɹʇןn uou 'oɹǝqıן ɯıuǝ snʇɔnן pǝs ˙soɹǝ ɯnʇuǝɯǝןǝ sınb snɯɐʌıʌ˙nǝ sıʇʇɐɯ snʇɔǝן ɐıuıɔɐן uou 'ɯɐnb soɹǝ opoɯɯoɔ uɐǝuǝɐ ˙ɐuɹn nǝ ʇɐıbnǝɟ ɯnɹʇnɹ ɐʇɹod ıɯ pǝs ǝnbǝu ǝnbuoɔ uıoɹd ˙ınp ɯnıʇǝɹd ɐɹɹǝʌıʌ 'uı ɐuɹn ʇn ɐɹʇǝɹɐɥd 'ǝʇuɐ ʇsǝ ǝɔsnɟ ˙ɯnsdı ʇıןǝ ɐ ɯɐıʇǝ ˙ɹoʇɹoʇ ʇǝǝɹoɐן ǝɐʇıʌ pǝs ˙ǝnbsǝʇuǝןןǝd ǝɐʇıʌ ɥqıu snsɹnɔ ɐɹɹǝʌıʌ ıqɹoɯ ˙poɯsınǝ ǝnbsǝʇuǝןןǝd snɹnd ɹoʇɔnɐ ʇǝ 'snɔɐן soɹǝ ʇɐdʇnןoʌ snןןǝsɐɥd ˙ʇıɹǝɹpuǝɥ ɐןןnu poɯsınǝ nǝ 'puǝɟıǝןǝ oǝן sınb ɐuɹn sıןןɐʌuoɔ ǝnbsǝʇuǝןןǝd ˙snqıɔnɐɟ uou snʇɔǝן uɐsɯnɔɔɐ ǝɹɐuɹo sıɹnɐɯ˙oɹǝqıן ʇǝ ısıu ɐןnbıן ɹɐuıʌןnd ɐ 'xǝ ɯnʇuǝɯɹǝɟ ɯnsdı sıןǝɟ 'ɐıuıɔɐן ɐpıʌɐɹb ʇn ɐןןnu 'snɔuoɥɹ uɐǝuǝɐ ˙ʇǝıpɹǝdɯı ʇǝǝɹoɐן ɯɐıp pı snqıuıɟ ɹǝbǝʇuı ˙sıdɹnʇ ʇɐnbǝsuoɔ 'pı xǝ ʇǝıpɹǝdɯı 'ɐןnɔıɥǝʌ uǝıdɐs pı sınp ˙snʇǝɯ ʇǝǝɹoɐן ʇǝɯɐ ʇıs 'ǝʇuɐ ɯnıʇǝɹd ɐ ʇn ˙ıɯ ʇǝ sıʇʇɐɯ 'ןǝʌ ʇıɹǝɹpuǝɥ ʇɐ ɐpıʌɐɹb 'soɹǝ ɯɐıp sıɹnɐɯ ˙ɐɹʇǝɹɐɥd ʇǝǝɹoɐן ɥqıu ʇǝɯɐ ʇıs snɹnd ǝɐʇıʌ sɐuǝɔǝɐɯ ˙sınb ɐpɐnsǝןɐɯ ɐuɹn ɯnʇuǝɯǝןǝ ɹodɯǝʇ 'ɹoʇɹoʇ snʇǝɯ sɐʇsǝbǝ ʇuǝsǝɐɹd ˙snʇɔnן ןǝʌ snןןǝʇ uɐsɯnɔɔɐ ǝʇɐʇndןnʌ ɹǝbǝʇuı ˙uı ɹnʇǝʇɔǝsuoɔ ʇıןǝʌ ɯıssıubıp uou 'nɔɹɐ ʇɐdʇnןoʌ sıןnɔɐı ǝnbsınb ˙ɯɐnb ɔunu ʇɐıbnǝɟ ǝssıpuǝdsns ˙ǝnbǝu ɔǝu ɯnןnqıʇsǝʌ 'sınb ʇǝıpɹǝdɯı ʇǝbǝ ɹodɯǝʇ 'ısıu ısıu ʇuǝsǝɐɹd ˙ɯıuǝ opoɯɯoɔ ʇǝ sıɹnɐɯ˙snsıɹ ʇsǝ uou ʇn ˙ɥqıu uou ʇɐ ɹǝdɹoɔɯɐןןn ǝʇɐʇndןnʌ ıɔɹo nǝ uǝıdɐs ʇɐ ɹnʇıqɐɹnɔ ˙ʇǝbǝ ǝıʇsǝןoɯ ıɔɹo ɹoʇıʇʇɹod ןǝʌ 'ıɔɹo ɐssɐɯ ɐpɐnsǝןɐɯ pǝs ˙sıʇɹoqoן ɔǝu ɹoןop uɐsɯnɔɔɐ snʇɔnן ʇn ˙nɔɹɐ uı ǝɹɐuɹo 'ʇn ɹnʇǝʇɔǝsuoɔ ɔǝu ɯnıʇǝɹd 'ɥqıu ʇɐɹǝ ɯɐu ˙sısıןıɔɐɟ uɐsɯnɔɔɐ ʇıɹǝɹpuǝɥ uıoɹd ˙ıɯ ʇǝɯɐ ʇıs ǝɐʇıʌ ɯnʇuǝɯıpuoɔ ʇɐdʇnןoʌ nɔɹɐ ʇǝɯɐ ʇıs oɹǝqıן sıʇɐuǝuǝʌ sıɹnɐɯ ˙ɯɐıp ɹoʇɔnɐ sınb 'ǝnbǝu ʇıɹǝɹpuǝɥ nǝ ɔǝuop ˙ʇǝıpɹǝdɯı ɐʇɹod uı ɐssɐɯ ɯnıʇǝɹd ɐןןnu ˙oǝן sndɯǝʇ ǝɐʇıʌ 'ɯɐnb ɯɐnbıןɐ ʇǝbǝ ıqɹoɯ ˙soɹǝ nɔɹɐ ʇǝɯɐ ʇıs snןןǝsɐɥd ˙sıʇʇɐɯ ɐʇɹod ɯnsdı ǝɐʇıʌ oʇsnظ ɔǝu ɯɐnbıןɐ ˙oǝן ɯıuǝ ʇǝıpɹǝdɯı ɯɐןןnu ˙ǝʇuɐ ʇɐɹǝ ɔɐ ǝnbsınb˙ɹǝdɯǝs ʇǝbǝ ʇıןǝʌ ןǝʌ uɐsɯnɔɔɐ uı ˙uɐsɯnɔɔɐ ǝnbǝu ɐןnɔıɥǝʌ nǝ 'ɯnʇuǝɯǝןǝ ʇsǝ uou oıpo ɐןnɔıɥǝʌ ǝɔsnɟ ˙ǝnbsıɹǝןǝɔs sıʇɹoqoן ɐssɐɯ ʇn ɯɐnb sınb ɔǝuop ˙snןןǝʇ ʇɐdʇnןoʌ ʇǝbǝ ıqɹoɯ ˙uǝıdɐs ǝıʇsǝןoɯ sıןןoɯ 'uɐsɯnɔɔɐ sıdɹnʇ pı ɯnʇuǝɯǝןǝ 'ınp snʇǝɯ ʇuǝsǝɐɹd ˙soǝɐuǝɯıɥ soʇdǝɔuı ɹǝd 'ɐɹʇsou ɐıqnuoɔ ɹǝd ʇuǝnbɹoʇ ɐɹoʇıן pɐ nbsoıɔos ıʇıɔɐʇ ʇuǝʇdɐ ssɐןɔ ˙sıןnɔɐı ɹɐuıʌןnd ʇsǝ nǝ snʇǝɯ ɯıssıubıp snןןǝsɐɥd ˙ɐןןıbuıɹɟ nǝ snɹnd nǝ ǝnbsıɹǝןǝɔs ʇuǝsǝɐɹd ˙snɹnd ǝnbsıɹǝןǝɔs ǝɐʇıʌ ɯɐnbıןɐ ˙sǝɔıɹʇןn ʇɐ ɯıuǝ pı snʇɔnן snןןǝsɐɥd ˙ʇn sǝɔıɹʇןn snɔɐן ɐןןıbuıɹɟ ɔɐ 'oıpo ısıu snıɹɐʌ uı ˙snɯıxɐɯ ɹnʇǝʇɔǝsuoɔ ɐןןnu ɐ uǝıdɐs ʇn ʇuǝsǝɐɹd ˙ʇɐɹǝ uı uou ǝʇɐʇndןnʌ sndɯǝʇ snɔɐן ɔɐ snsıɹ ןǝʌ snןןǝsɐɥd˙sıןǝɟ ɐssɐɯ uı ɯɐnbıןɐ ˙ɯɐıp sıןןoɯ ʇıpuɐןq 'ǝɐʇıʌ ɯǝɹoן ɐ snqıdɐp 'ınp ǝnbǝu ɔǝuop ˙sǝןɐpos ɯnʇuǝɯɹǝɟ soɹǝ ǝɐʇıʌ snsıɹ snɯıxɐɯ ɯɐu ˙ɐssɐɯ sıןןoɯ pǝs 'ןsıu ɐɹʇǝɹɐɥd ןǝʌ ʇn ˙uou ɯnʇuǝɯıpuoɔ ɯɐıp ɹnʇıɔıɟɟǝ ʇıpuɐןq 'ɐןןnu xǝ ɹodɯǝʇ sınp ˙ǝnbnɐ poɯsınǝ ɔǝu ǝɔsnɟ ˙oıpo sıʇʇıbɐs ɐɹʇǝɹɐɥd ıqɹoɯ ˙sıʇʇıbɐs snqıɔnɐɟ snʇǝɯ sǝɔıɹʇןn snsıɹ ʇıɹǝɹpuǝɥ ʇuǝsǝɐɹd ˙sıdɹnʇ sıןnɔɐı sıןןɐʌuoɔ 'ʇǝbǝ nɔɹɐ ʇn ɯnpɹǝʇuı 'oɹǝqıן ɐןןnu ɹǝbǝʇuı˙ʇunpıɔuıʇ ʇǝ ǝnbǝu sǝןɐpos snqıdɐp ɯɐıʇǝ ˙sıdɹnʇ ǝʇɐʇndןnʌ ʇǝbǝ 'ɯǝs snqıɔnɐɟ ɯnɹʇnɹ ǝɔsnɟ ˙snɯıxɐɯ ʇǝɯɐ ʇıs snsıɹ ɯnʇuǝɯǝןǝ ɯnʇɔıp ǝnbsınb ˙ǝnbsıɹǝןǝɔs ʇǝɯɐ ʇıs nɔɹɐ ɹoʇıʇʇɹod ɐıuıɔɐן ǝssıpuǝdsns ˙ʇıןǝ buıɔsıdıpɐ ɹnʇǝʇɔǝsuoɔ 'ʇǝɯɐ ʇıs ɹoןop ɯnsdı ɯǝɹoן ˙ɹɐuıʌןnd ǝɹǝnsod snɔuoɥɹ ɯɐu ˙ʇıןǝ buıɔsıdıpɐ ɹnʇǝʇɔǝsuoɔ 'ʇǝɯɐ ʇıs ɹoןop ɯnsdı ɯǝɹoן ˙sıןןoɯ pı snʇɔǝן ɔɐ ʇɐdʇnןoʌ ǝnbsınb ˙snןןǝʇ sısıןıɔɐɟ ʇn 'nɔɹɐ uıpnʇıɔıןןos pı ɹnʇıqɐɹnɔ˙ɯɐnbıןɐ ɔǝu oıpo pı ʇɐnbǝsuoɔ uı ˙ʇɐ ʇɐɹǝɔɐןd ɯǝs ɯnpɹǝʇuı pǝs 'oʇsnظ ɐןןıbuıɹɟ snsɹnɔ ıqɹoɯ ˙ɯnןnqıʇsǝʌ ɹoʇıʇʇɹod ʇǝbǝ ɯɐnb ɯɐnbıןɐ ɔǝuop ˙sǝןɐpos snɯıxɐɯ snıɹɐʌ ǝnbsǝʇuǝןןǝd ˙snqıɔnɐɟ uı sıɯıɹd ɯnsdı ǝʇuɐ ɔɐ sǝɯɐɟ ɐpɐnsǝןɐɯ ʇǝ ɯnpɹǝʇuı ˙sǝןɐpos oıpo snıɹɐʌ pǝs 'sıʇʇɐɯ sıdɹnʇ ʇɐ snɹnd ǝɹɐuɹo ǝɔsnɟ ˙snɯ snןnɔıpıɹ ɹnʇǝɔsɐu 'sǝʇuoɯ ʇuǝıɹnʇɹɐd sıp sıubɐɯ ʇǝ snqıʇɐuǝd ǝnboʇɐu sııɔos ɯnɔ ˙uǝıdɐs ʇǝbǝ ɔǝu ɯnʇuǝɯıpuoɔ poɯsınǝ snsıɹ pǝs ıɔɹo ɐ pǝs ˙uıpnʇıɔıןןos snɹnd ɹoʇıʇʇɹod ɔǝu 'ɯnʇɔıp ɯɐnb sınb soɹǝ ǝnbsǝʇuǝןןǝd uıoɹd ˙ʇunpıɔuıʇ snɔuoɥɹ sǝıɔıɹʇןn ɯnןnqıʇsǝʌ ˙snsıɹ ʇǝnbıןɐ ɯɐnbıןɐ snɯɐʌıʌ ˙ʇıןǝ buıɔsıdıpɐ ɹnʇǝʇɔǝsuoɔ 'ʇǝɯɐ ʇıs ɹoןop ɯnsdı ɯǝɹoן";
    protected static List<POI> m_pois = getPois();
    protected static byte[] m_someBytes = m_bigStr.getBytes();
    protected static byte[] m_someBytesFlipped = m_bigStrFlipped.getBytes();

    /**
     * Amount of loops per test
     */
    public static final int LOOP = 10;

    /**
     * Tests that we can call the correct methods for a union type
     */
    public void testPerformance() throws Exception {
        System.out.println("PERFORMANCE TEST for " + getType() + " loop: " + LOOP);
        PerformanceTestInterface proxy = getProxy(PerformanceTestInterface.class);
        StopWatch stopWatch = new StopWatch();

        // Run first test case to init all components
        setUpURLStreamHandler(new TestURLConnection(HTTP_OK, getTestString("Test")));
        stopWatch.start();
        String result = proxy.flip(m_bigStr);
        stopWatch.stop();
        assertEquals("Test", result);
        System.out.println(getType() + "-Init: " + stopWatch.getTime() + " (" + stopWatch.getNanoTime() + ")");

        // BigString
        byte[] responseMsg = getFlipRespnse(m_bigStrFlipped);
        String flippedStr = null;
        prepare(stopWatch);
        for (int i = 0; i < LOOP; i++) {
            setUpURLStreamHandler(new TestURLConnection(HTTP_OK, responseMsg));
            stopWatch.resume();
            flippedStr = proxy.flip(m_bigStr);
            stopWatch.suspend();
            assertEquals(m_bigStrFlipped, flippedStr);
        }
        stopWatch.stop();
        System.out.println(getType() + "-BigString: " + stopWatch.getTime() + " (" + stopWatch.getNanoTime()
            + ", responseSize:" + responseMsg.length + ")");

        // Complex objects
        responseMsg = getPoisRespnse(m_pois);
        prepare(stopWatch);
        List<POI> pois = null;
        for (int i = 0; i < LOOP; i++) {
            setUpURLStreamHandler(new TestURLConnection(HTTP_OK, responseMsg));
            stopWatch.resume();
            pois = proxy.poiInRange(40.7143528f, -74.0059731f, 1337);
            stopWatch.suspend();
            assertEquals(m_pois, pois);
        }
        stopWatch.stop();
        System.out.println(getType() + "-ComplexObjects: " + stopWatch.getTime() + " (" + stopWatch.getNanoTime()
            + ", responseSize:" + responseMsg.length + ")");

        responseMsg = getSomeFloatResponse(Float.MAX_VALUE);
        prepare(stopWatch);
        float f = -1;
        for (int i = 0; i < LOOP; i++) {
            setUpURLStreamHandler(new TestURLConnection(HTTP_OK, responseMsg));
            stopWatch.resume();
            f = proxy.someFloat(Float.MIN_VALUE);
            stopWatch.suspend();
            assertEquals(Float.MAX_VALUE, f);
        }
        stopWatch.stop();
        System.out.println(getType() + "-FloatMinMax: " + stopWatch.getTime() + " (" + stopWatch.getNanoTime()
            + ", responseSize:" + responseMsg.length + ")");

    }

    private static void prepare(final StopWatch stopWatch) {
        stopWatch.reset();
        stopWatch.start();
        stopWatch.suspend();
    }

    protected abstract byte[] getTestString(String result);

    protected abstract byte[] getSomeFloatResponse(float result);

    protected abstract byte[] getPoisRespnse(List<POI> result);

    protected abstract byte[] getFlipRespnse(String result);

    protected abstract <T> T getProxy(Class<T> intrfc);

    protected abstract String getType();

    private static final List<POI> getPois() {
        List<POI> pois = new ArrayList<POI>();
        pois.add(new POI(40.7115405f, -74.0132725f, "9/11 Memorial", 10));
        pois.add(new POI(40.7813241f, -73.9739882f, "American Museum of Natural History", 9));
        pois.add(new POI(40.7060855f, -73.9968643f, "Brooklyn Bridge", 8));
        pois.add(new POI(40.6712062f, -73.9636306f, "Brooklyn Museum", 7.5));
        pois.add(new POI(40.767778f, -73.9718335f, "Central Park Zoo", 1.337));
        return pois;
    }

    protected interface PerformanceTestInterface {
        String flip(String string);

        List<POI> poiInRange(float latitude, float longitude, int range);

        float someFloat(float f);
    }

    protected static class POI {
        public float m_latitude;
        public float m_longitude;
        public String m_name;
        public double m_score;

        public POI() {
        }

        POI(final float latitude, final float longitude, final String name, final double score) {
            m_latitude = latitude;
            m_longitude = longitude;
            m_name = name;
            m_score = score;
        }

        @Override
        public boolean equals(final Object obj) {
            if (obj instanceof POI) {
                POI other = (POI) obj;
                if (m_latitude == other.m_latitude) {
                    if (m_longitude == other.m_longitude) {
                        if (m_score == other.m_score) {
                            if (m_name.equals(other.m_name)) {
                                return true;
                            }
                        }
                    }
                }
            }
            return false;
        }
    }

}
