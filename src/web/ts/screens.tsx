import { VNode, h } from "preact";
import { dialogueBox, errorView, infoDescription } from "./styles.css";

const githubLink =
  <div class={infoDescription}>
    <p>
      Think you've found a bug? Have a suggestion? Why not put it
      on <a href="https://github.com/SquidDev-CC/" title="The GitHub repository">the GitHub repo</a>?
    </p>
  </div>;

export const UnknownError = ({ error }: { error: string }): VNode<unknown> =>
  <div class={infoDescription}>
    <div class={`infoView ${errorView}`}>
      <h2>An error occured</h2>
      <pre>{error}</pre>
    </div>
    {githubLink}
  </div>;

export const About = (): VNode<unknown> => <div class={dialogueBox}>
  <h2>About</h2>
  <p>
    Copy Cat is a web emulator for the popular Minecraft mod <a href="https://github.com/SquidDev-CC/CC-Tweaked"
      target="_blank" title="CC: Tweaked's source code">CC: Tweaked</a> (based on ComputerCraft by Dan200). Here you can
    play with a ComputerCraft computer, write and test programs and experiment to your heart's desire, without having to
    leave your browser!
  </p>

  <p>
    However, due to the limitations of Javascript, some functionality may not be 100% accurate (most notably, that to do
     with HTTP and filesystems). For even closer emulation, I'd recommend <a href="https://emux.cc/" target="_blank"
      title="The CCEmuX emulator">CCEmuX</a>.
  </p>

  <p>
    If you need help writing a program, I'd recommend checking out the <a href="https://forums.computercraft.cc/"
      target="_blank" title ="The CC: Tweaked forums">CC: Tweaked</a> or <a href="https://discord.gg/H2UyJXe"
      title="The Minecraft Computer Mods Discord" target="_blank">ComputerCraft</a> forums. <a
      href="https://tweaked.cc" target="_blank" title="The CC: Tweaked wiki">The CC: Tweaked
    wiki</a> may also be a good source of documentation.
  </p>

  <p>
    Of course, this emulator is sure to have lots of bugs and missing features. If you've found a problem, why not put
    it on <strong><a href="https://github.com/SquidDev-CC/copy-cat/issues" title="The Copy Cat GitHub issue tracker">the
    GitHub repo</a></strong>?
  </p>

  <h3>Credits</h3>
  <p>
    Copy Cat would not be possible without the help of several Open Source projects.
  </p>

  <ul>
    <li><a href="https://github.com/konsoletyper/teavm" target="_blank">TeaVM</a>: Apache 2.0</li>
    <li><a href="https://github.com/google/guava" target="_blank">Google Guava</a>: Apache 2.0</li>
    <li>
      <a href="https://github.com/apache/commons-lang" target="_blank">Apache Commons Lang</a>
      : Apache 2.0, Copyright 2001-2018 The Apache Software Foundation
    </li>
    <li>
      <a href="https://github.com/SquidDev/Cobalt" target="_blank">Cobalt/LuaJ</a>
      : MIT, Copyright (c) 2009-2011 Luaj.org. All rights reserved., modifications Copyright (c) 2015-2016 SquidDev
    </li>
    <li>
      <a href="https://github.com/SquidDev-CC/CC-Tweaked" target="_blank">CC: Tweaked</a>
      : ComputerCraft Public License
    </li>
    <li>
      <a href="https://github.com/FortAwesome/Font-Awesome/" target="_blank">Font Awesome</a>
      : CC BY 4.0
    </li>
    <li>
      Numerous Javascript libraries. A full list can be found <a href="dependencies.txt" target="_blank">in the
      dependencies list</a> or at the top of any Javascript file.
    </li>
  </ul>

  <pre>
    {
      `This product includes software developed by Alexey Andreev (http://teavm.org).

This product includes software developed by The Apache Software Foundation (http://www.apache.org/).

This product includes software developed by Joda.org (http://www.joda.org/).`
    }
  </pre>
</div>;
