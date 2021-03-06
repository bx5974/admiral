package tags

import (
	"admiral/client"
	"admiral/config"
	"admiral/utils"
	"bytes"
	"encoding/json"
	"fmt"
	"net/http"
	"strings"
)

type TagError struct {
	invalidInput string
}

func (te *TagError) Error() string {
	msg := "Invalid tag format for input: %s. \"Use key:value\" format."
	return fmt.Sprintf(msg, te.invalidInput)
}

func NewTagError(input string) *TagError {
	return &TagError{
		invalidInput: input,
	}
}

type Tag struct {
	Key              string `json:"key"`
	Value            string `json:"value"`
	DocumentSelfLink string `json:"documentSelfLink,omitempty"`
}

func (t *Tag) String() string {
	return fmt.Sprintf("[%s:%s]", t.Key, t.Value)
}

func (t *Tag) GetID() string {
	return utils.GetResourceID(t.DocumentSelfLink)
}

func (t *Tag) SetKeyValues(input string) error {
	var key, val string

	if !strings.Contains(input, ":") {
		key = input
		val = ""
	} else {
		keyValArr := strings.Split(input, ":")
		if len(keyValArr) != 2 {
			return NewTagError(input)
		}
		key = strings.TrimSpace(keyValArr[0])
		val = strings.TrimSpace(keyValArr[1])
		if key == "" {
			return NewTagError(input)
		}
	}
	t.Key = key
	t.Value = val
	return nil
}

func NewTag(input string) (*Tag, error) {
	tag := &Tag{}
	err := tag.SetKeyValues(input)
	return tag, err
}

type TagList struct {
	DocumentLinks []string       `json:"documentLinks"`
	Documents     map[string]Tag `json:"documents"`
}

func (tl *TagList) GetCount() int {
	return len(tl.Documents)
}

func GetTagIdByEqualKeyVals(input string, createIfNotExist bool) (string, error) {
	tagToMatch, err := NewTag(input)
	if err != nil {
		return "", err
	}

	filterUrl := config.URL + "/resources/tags?documentType=true&expand=true&$filter=key+eq+'%s'+and+value+eq+'%s'"
	filterUrl = fmt.Sprintf(filterUrl, tagToMatch.Key, tagToMatch.Value)

	req, _ := http.NewRequest("GET", filterUrl, nil)
	_, respBody, respErr := client.ProcessRequest(req)
	if respErr != nil {
		return "", respErr
	}

	tagList := &TagList{}
	err = json.Unmarshal(respBody, tagList)
	if err != nil {
		return "", err
	}

	if tagList.GetCount() < 1 {
		if createIfNotExist {
			return AddTag(tagToMatch)
		}
		return "", nil
	}

	tag := tagList.Documents[tagList.DocumentLinks[0]]
	return tag.GetID(), nil
}

func AddTag(tag *Tag) (string, error) {
	url := config.URL + "/resources/tags/"
	jsonBody, err := json.Marshal(tag)
	utils.CheckBlockingError(err)
	req, _ := http.NewRequest("POST", url, bytes.NewBuffer(jsonBody))
	_, respBody, respErr := client.ProcessRequest(req)
	if respErr != nil {
		return "", respErr
	}
	newTag := &Tag{}
	err = json.Unmarshal(respBody, newTag)
	utils.CheckBlockingError(err)
	return newTag.GetID(), nil
}

func TagsToString(tagLinks []string) string {
	if len(tagLinks) == 0 {
		return "n/a"
	}
	var buffer bytes.Buffer
	for _, tl := range tagLinks {
		tag := getTag(tl)
		buffer.WriteString(tag.String())
	}
	return buffer.String()
}

func getTag(link string) *Tag {
	url := config.URL + link
	req, _ := http.NewRequest("GET", url, nil)
	_, respBody, _ := client.ProcessRequest(req)
	tag := &Tag{}
	json.Unmarshal(respBody, tag)
	return tag
}
