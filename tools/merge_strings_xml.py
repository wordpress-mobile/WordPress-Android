#!/usr/bin/env python

import logging, sys
import xml.etree.ElementTree as ET
from xml.dom import minidom
import json

xml_indent = '    ';
#logging.basicConfig(stream=sys.stderr, level=logging.DEBUG)
logging.basicConfig(stream=sys.stderr, level=logging.WARNING)

class CustomTreeBuilder(ET.TreeBuilder):
    def comment(self, text):
        self.start(ET.Comment, {})
        self.data(text)
        self.end(ET.Comment)

def xml_parse(xml_file):
    root = None
    ns_map = {} # prefix -> ns_uri
    for event, elem in ET.iterparse(xml_file, ['start-ns', 'start', 'end'], parser=ET.XMLParser(target=CustomTreeBuilder())):
        if event == 'start-ns':
            # elem = (prefix, ns_uri)
            ns_map[elem[0]] = elem[1]
        elif event == 'start':
            if root is None:
                root = elem
    for prefix, uri in ns_map.items():
        ET.register_namespace(prefix, uri)
        
    return ET.ElementTree(root)

def get_string_names(tree):
    names = [element.attrib["name"] if "name" in element.attrib else None for element in tree]
    return dict.fromkeys(names, 1)

def create_section(tree_root, section_name):
    insertion_point_index = len(tree_root)
    previous_element = list(tree_root)[insertion_point_index-1]
    previous_element.tail = ('\n'+xml_indent) * 3
    start_section_comment = ET.Comment(' '+section_name+' ')
    start_section_comment.tail = '\n'+xml_indent
    tree_root.insert(insertion_point_index, start_section_comment)
    end_section_comment = ET.Comment(' END ('+section_name+') ')
    end_section_comment.tail = '\n'
    insertion_point_index = insertion_point_index + 1
    tree_root.insert(insertion_point_index, end_section_comment)
    return insertion_point_index

def add_section(tree_root, insertion_point_index, section_name, new_elements):
    if insertion_point_index is None:
        insertion_point_index = create_section(tree_root, section_name)
    # remove existing strings in main xml
    string_names = get_string_names(tree_root)
    new_elements_filtered = [element for element in new_elements if element.tag is ET.Comment or "name" in element.attrib and element.attrib["name"] not in string_names]
    for index, new_element in enumerate(new_elements_filtered):
        tree_root.insert(insertion_point_index+index, new_element)
    new_elements_filtered[-1].tail = '\n'+xml_indent;

def find_and_empty_section(tree_root, section_name):
    removing = False
    for index, resource_element in enumerate(tree_root):
        if resource_element.tag is ET.Comment and resource_element.text == ' '+section_name+' ':
            removing = True
            logging.debug( "Emptying section <!-- %s -->" % section_name )
        if resource_element.tag is ET.Comment and resource_element.text == ' END ('+section_name+') ':
            return index
        if removing:
            tree_root.remove(resource_element)
    logging.debug( 'Adding a new setion <!-- %s -->' % section_name )
    return None

def merge_strings(main_xml, extra_sections):
    xml_output_tree = xml_parse(main_xml)
    main_root = xml_output_tree.getroot()
    for extra_section in extra_sections:
        section_tree = xml_parse(extra_section['file'])
        section_name = extra_section['name']
        new_elements = list(section_tree.getroot().iter('resources'))[0]
        insertion_point_index = find_and_empty_section(main_root, section_name)
        add_section(main_root, insertion_point_index, section_name, new_elements)
    xml_output_tree.write(main_xml, encoding='utf-8', xml_declaration=True)

def main():
    merge_strings(
        './WordPress/src/main/res/values/strings.xml',
        [
            { 'name': 'Login', 'file': './libs/login/WordPressLoginFlow/src/main/res/values/strings.xml' },
            { 'name': 'Gutenberg Native', 'file': './libs/gutenberg-mobile/bundle/android/strings.xml' },
        ]
    )

if __name__ == "__main__":
    main()
