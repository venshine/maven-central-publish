/*
 * Copyright (C) 2016 venshine.cn@gmail.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.github.venshine.sample;

/**
 * 示例类：只用来让示例库产出非空的 aar、sources 和 javadoc。
 */
public final class Greeter {

    private Greeter() {
    }

    /**
     * 生成问候语。
     *
     * @param name 名字，为空时按 "world" 处理
     * @return 问候语
     */
    public static String greet(String name) {
        String target = (name == null || name.trim().isEmpty()) ? "world" : name.trim();
        return "Hello, " + target + "!";
    }
}
